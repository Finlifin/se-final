defmodule FlixBackendWeb.MessageChannel do
  use Phoenix.Channel
  alias FlixBackend.Accounts.Account
  alias FlixBackend.Data.{Message, Event, Conversation, UserConversation}
  alias FlixBackend.Guardian
  alias FlixBackend.Repo
  import Ecto.Query

  # 加入用户专属的消息通道
  def join("user:" <> user_id, %{"token" => token}, socket) do
    case Guardian.decode_and_verify(token) do
      {:ok, claims} ->
        account_id = claims["sub"]
        account = Account.get_account!(account_id)
        if user_id == account.user_id do
          {:ok, socket |> assign(:user_id, user_id)}
        else
          {:error, %{reason: "unauthorized"}}
        end

      {:error, reason} ->
        IO.inspect(reason, label: "Token Decode Error")
        {:error, %{reason: "invalid token"}}
    end
  end

  # 拒绝所有其他通道
  def join(topic, payload, _socket) do
    IO.inspect(payload, label: "Invalid Join Attempt")
    {:error, %{reason: "Invalid Join Attempt, channel: #{topic}"}}
  end

  # 处理同步请求
  def handle_in("sync", %{"last_sync_timestamp" => last_sync_timestamp}, socket) do
    user_id = socket.assigns.user_id
    IO.inspect(user_id, label: "User ID")

    # 解析时间戳
    timestamp = case DateTime.from_iso8601(last_sync_timestamp) do
      {:ok, datetime, _} -> datetime
      {:error, _} -> DateTime.now("Etc/UTC")
    end

    # 获取该用户的新事件
    events = Event.get_events_since(user_id, timestamp)

    # 确定新的同步时间戳
    new_last_sync_timestamp = if Enum.empty?(events) do
      DateTime.utc_now() |> DateTime.to_iso8601()
    else
      latest_event = Enum.max_by(events, &(&1.event_timestamp))
      latest_event.event_timestamp |> DateTime.to_iso8601()
    end

    {:reply, {:ok, %{
      events: events,
      new_last_sync_timestamp: new_last_sync_timestamp
    }}, socket}
  end

  # 发送消息
  def handle_in("send_private_message", payload, socket) do
    user_id = socket.assigns.user_id

    # 确保有必要的参数
    with {:ok, client_message_id} <- Map.fetch(payload, "client_message_id"),
         {:ok, conversation_id} <- Map.fetch(payload, "conversation_id"),
         {:ok, content} <- Map.fetch(payload, "content"),
         {:ok, message_type} <- Map.fetch(payload, "message_type"),
         {:ok, client_timestamp} <- Map.fetch(payload, "client_timestamp") do

      # 生成服务器端消息ID和时间戳
      message_id = Ecto.UUID.generate()
      server_timestamp = DateTime.utc_now()
      parsed_client_timestamp = case DateTime.from_iso8601(client_timestamp) do
        {:ok, dt, _} -> dt
        {:error, _} -> server_timestamp
      end
      {:ok, content} = Jason.encode(content)
      {:ok, content} = Jason.decode(content)

      # 查找会话和接收者
      conversation = Repo.get_by(Conversation, id: conversation_id)

      if conversation do
        # 创建消息记录
        message_params = %{
          message_id: message_id,
          client_message_id: client_message_id,
          conversation_id: conversation_id,
          sender_id: user_id,
          content: content,
          message_type: message_type,
          status: :sent,
          server_timestamp: server_timestamp,
          client_timestamp: parsed_client_timestamp
        }

        # 插入消息
        IO.inspect(message_params, label: "Message Params")
        changeset = Message.changeset(%Message{}, message_params)

        case Repo.insert(changeset) do
          {:ok, message} ->
            # 更新会话的最后消息信息
            update_conversation_last_message(conversation.id, message)

            # 对每个参与者创建事件(除了发送者)
            Enum.each(conversation.participant_ids, fn participant_id ->
              if participant_id != user_id do
                create_and_broadcast_event("new_message", message, participant_id)
              end
            end)

            # 回复发送方确认
            {:reply, {:ok, %{
              client_message_id: client_message_id,
              message_id: message_id,
              server_timestamp: DateTime.to_iso8601(server_timestamp),
              status: "sent"
            }}, socket}

          {:error, changeset} ->
            IO.inspect(changeset, label: "Message Insert Error")
            {:reply, {:error, %{errors: error_messages(changeset)}}, socket}
        end
      else
        {:reply, {:error, %{reason: "conversation not found"}}, socket}
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing required parameters"}}, socket}
    end
  end

  # 标记消息已读
  def handle_in("mark_read", %{"conversation_id" => conversation_id, "last_read_message_id" => last_read_message_id}, socket) do
    user_id = socket.assigns.user_id

    # 查找会话
    conversation = Repo.get_by(Conversation, conversation_id: conversation_id)

    if conversation do
      # 更新用户会话关系中的已读消息ID
      user_conversation = Repo.get_by(UserConversation,
        user_id: user_id, conversation_id: conversation_id)

      if user_conversation do
        # 更新用户会话的已读状态
        Repo.update(UserConversation.changeset(user_conversation, %{
          last_read_message_id: last_read_message_id,
          unread_count: 0
        }))
      end

      # 查找需要更新状态的消息
      message_query = from m in Message,
        where: m.conversation_id == ^conversation_id and
               m.message_id <= ^last_read_message_id and
               m.status == :unread and
               m.receiver_id == ^user_id

      {updated_count, updated_messages} = Repo.update_all(
        message_query,
        [set: [status: :read, updated_at: DateTime.utc_now()]],
        returning: true
      )

      # 为每条更新的消息创建状态更新事件
      Enum.each(updated_messages, fn message ->
        if message.sender_id && message.sender_id != user_id do
          create_and_broadcast_event("message_status_update", %{
            message_id: message.message_id,
            conversation_id: conversation_id,
            status: "read",
            updated_at: DateTime.utc_now()
          }, message.sender_id)
        end
      end)

      {:reply, {:ok, %{updated_count: updated_count}}, socket}
    else
      {:reply, {:error, %{reason: "conversation not found"}}, socket}
    end
  end

  # 撤回消息
  def handle_in("withdraw_message", %{"message_id" => message_id}, socket) do
    user_id = socket.assigns.user_id

    case Message.withdraw_message(message_id, user_id) do
      {:ok, updated_message} ->
        # 查询消息所属的会话
        conversation = Repo.get_by(Conversation, conversation_id: updated_message.conversation_id)

        if conversation do
          # 为会话中的每个参与者创建撤回事件
          Enum.each(conversation.participant_ids, fn participant_id ->
            create_and_broadcast_event("message_recalled", %{
              message_id: message_id,
              conversation_id: updated_message.conversation_id,
              status: "withdrawn",
              updated_at: DateTime.utc_now()
            }, participant_id)
          end)
        end

        {:reply, {:ok, %{status: "withdrawn"}}, socket}

      {:error, :not_found} ->
        {:reply, {:error, %{reason: "message not found"}}, socket}

      {:error, :time_expired} ->
        {:reply, {:error, %{reason: "cannot withdraw message after time limit"}}, socket}

      {:error, changeset} ->
        {:reply, {:error, %{errors: error_messages(changeset)}}, socket}
    end
  end

  # 获取会话消息历史
  def handle_in("get_conversation_history", params, socket) do
    user_id = socket.assigns.user_id

    with {:ok, conversation_id} <- Map.fetch(params, "conversation_id") do
      limit = Map.get(params, "limit", 20)
      before_timestamp = Map.get(params, "before")

      # 验证用户是否在会话中
      conversation = Repo.get_by(Conversation, conversation_id: conversation_id)

      if conversation && Enum.member?(conversation.participant_ids, user_id) do
        query_opts = [limit: limit]

        query_opts = if before_timestamp do
          case DateTime.from_iso8601(before_timestamp) do
            {:ok, datetime, _} -> Keyword.put(query_opts, :before_timestamp, datetime)
            {:error, _} -> query_opts
          end
        else
          query_opts
        end

        messages = Message.get_conversation_messages(conversation_id, query_opts)

        {:reply, {:ok, %{messages: messages}}, socket}
      else
        {:reply, {:error, %{reason: "unauthorized"}}, socket}
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing conversation_id"}}, socket}
    end
  end

  # 处理创建新会话请求
  def handle_in("create_conversation", params, socket) do
    user_id = socket.assigns.user_id

    with {:ok, type} <- Map.fetch(params, "type"),
         {:ok, participant_ids} <- Map.fetch(params, "participant_ids") do

      # 验证会话类型
      if type not in ["private", "group"] do
        {:reply, {:error, %{reason: "invalid conversation type"}}, socket}
      else
        # 为私聊添加当前用户到参与者列表
        all_participants = if type == "private" do
          # 确保参与者列表是正确的格式且包含当前用户
          [other_user_id] = participant_ids
          [user_id, other_user_id] |> Enum.uniq() |> Enum.sort()
        else
          # 群聊，确保当前用户在参与者中
          (participant_ids ++ [user_id]) |> Enum.uniq()
        end

        # 为私聊生成标准化的conversation_id
        conversation_id = case type do
          "private" ->
            [first_id, second_id] = all_participants
            "private:#{first_id}:#{second_id}"
          "group" ->
            # 对于群聊使用UUID
            "group:#{Ecto.UUID.generate()}"
        end

        # 检查会话是否已存在
        case Repo.get_by(Conversation, conversation_id: conversation_id) do
          nil ->
            # 创建新会话
            conversation_params = %{
              conversation_id: conversation_id,
              type: type,
              participant_ids: all_participants,
              updated_at: DateTime.utc_now()
            }

            # 事务：创建会话并为所有参与者创建用户会话关系
            result = Ecto.Multi.new()
              |> Ecto.Multi.insert(:conversation, Conversation.changeset(%Conversation{}, conversation_params))
              |> create_user_conversations(all_participants, conversation_id)
              |> Repo.transaction()

            case result do
              {:ok, %{conversation: conversation}} ->
                # 返回新建的会话信息
                {:reply, {:ok, %{conversation: conversation}}, socket}

              {:error, failed_operation, failed_value, _changes} ->
                {:reply, {:error, %{reason: "failed to create conversation: #{failed_operation}", details: error_messages(failed_value)}}, socket}
            end

          conversation ->
            # 会话已存在，直接返回
            {:reply, {:ok, %{conversation: conversation, already_exists: true}}, socket}
        end
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing required parameters"}}, socket}
    end
  end

  # 获取用户的会话列表
  def handle_in("get_conversations", params, socket) do
    IO.puts "Fetching conversations for user: #{socket.assigns.user_id}"
    user_id = socket.assigns.user_id
    limit = Map.get(params, "limit", 20)

    query = from c in Conversation,
      join: uc in UserConversation,
      on: c.conversation_id == uc.conversation_id,
      where: uc.user_id == ^user_id,
      order_by: [desc: c.updated_at],
      limit: ^limit,
      select: %{
        conversation: c,
        unread_count: uc.unread_count,
        is_pinned: uc.is_pinned,
        is_muted: uc.is_muted,
        last_read_message_id: uc.last_read_message_id
      }

    conversations = Repo.all(query)

    {:reply, {:ok, %{conversations: conversations}}, socket}
  end

  # 辅助函数：创建并广播事件
  defp create_and_broadcast_event(event_type, payload, target_user_id) do
    # 创建事件记录
    event_params = %{
      event_type: event_type,
      payload: payload,
      event_timestamp: DateTime.utc_now(),
      target_user_id: target_user_id
    }

    changeset = Event.changeset(%Event{}, event_params)

    case Repo.insert(changeset) do
      {:ok, event} ->
        # 广播事件给目标用户
        FlixBackendWeb.Endpoint.broadcast!(
          "user:#{target_user_id}",
          "event",
          event
        )

        # TODO: 如果用户不在线，触发外部推送

        {:ok, event}

      {:error, _} ->
        {:error, "failed to create event"}
    end
  end

  # 辅助函数：更新会话的最后一条消息
  defp update_conversation_last_message(conversation_id, message) do
    # 获取消息内容的文本表示
    message_preview = get_message_preview(message.content, message.content_type)

    conversation = Repo.get(Conversation, conversation_id)

    if conversation do
      # 更新会话信息
      Repo.update(Conversation.changeset(conversation, %{
        last_message_id: message.message_id,
        last_message_content: message_preview,
        last_message_timestamp: message.server_timestamp,
        updated_at: DateTime.utc_now()
      }))

      # 更新用户会话关系中的未读消息数量
      Enum.each(conversation.participant_ids, fn participant_id ->
        if participant_id != message.sender_id do
          user_conversation = Repo.get_by(UserConversation,
            user_id: participant_id, conversation_id: conversation.conversation_id)

          if user_conversation do
            UserConversation.increment_unread(user_conversation)
          end
        end
      end)
    end
  end

  # 辅助函数：为多个用户创建会话关系
  defp create_user_conversations(multi, participant_ids, conversation_id) do
    Enum.reduce(participant_ids, multi, fn user_id, multi ->
      user_conversation_params = %{
        user_id: user_id,
        conversation_id: conversation_id,
        unread_count: 0
      }

      Ecto.Multi.insert(
        multi,
        {:user_conversation, user_id},
        UserConversation.changeset(%UserConversation{}, user_conversation_params)
      )
    end)
  end

  # 辅助函数：获取消息预览文本
  defp get_message_preview(content, content_type) do
    # 解析JSON字符串到Map
    content_map = case Jason.decode(content) do
      {:ok, map} -> map
      {:error, _} -> %{}
    end

    case content_type do
      :text ->
        Map.get(content_map, "text", "")
        |> String.slice(0, 50)  # 限制预览长度

      :image -> "[图片消息]"
      :audio -> "[语音消息]"
      :video -> "[视频消息]"
      :product -> "[商品]" <> Map.get(content_map, "product_name", "")
      :order -> "[订单]" <> Map.get(content_map, "order_id", "")
      :comment -> "[评论]" <> Map.get(content_map, "text", "")
      :like -> "[点赞]"
      :favorite -> "[收藏]"
      :system -> Map.get(content_map, "title", "系统消息")
      _ -> "[消息]"
    end
  end

  # 辅助函数：提取表单错误信息
  defp error_messages(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      Enum.reduce(opts, msg, fn {key, value}, acc ->
        String.replace(acc, "%{#{key}}", to_string(value))
      end)
    end)
  end
end
