defmodule FlixBackend.Messaging do
  @moduledoc """
  The Messaging context.
  Provides functions for backend services to send various types of messages.
  """

  alias FlixBackend.Data.{Message, Event, Conversation, UserConversation}
  alias FlixBackend.Repo
  import Ecto.Query

  @doc """
  Sends a system notification to a specific user.

  ## Parameters

  - recipient_id: The ID of the user to receive the notification.
  - content_map: A map containing the notification content.
    Example: %{
      text: "Your account has been verified",
      title: "Account Verification",
      deep_link: "app://settings/account"
    }
  - reference_id: Optional ID to associate with this notification.

  ## Returns

  - {:ok, message} on success
  - {:error, changeset} on failure
  """
  def send_system_notification(recipient_id, content_map, reference_id \\ nil) do
    # 为系统通知创建一个专用的会话ID (如果不存在)
    conversation_id = "system:notification:#{recipient_id}"

    # 确保系统通知会话存在
    ensure_system_conversation(conversation_id, "system_notification", [recipient_id])

    # 生成消息参数
    message_params = %{
      message_id: Ecto.UUID.generate(),
      client_message_id: Ecto.UUID.generate(),
      conversation_id: conversation_id,
      sender_id: nil,
      receiver_id: recipient_id,
      content: content_map,
      message_type: :system_notification,
      reference_id: reference_id,
      server_timestamp: DateTime.utc_now(),
      client_timestamp: DateTime.utc_now(),
      status: :sent
    }

    create_and_broadcast_message(message_params)
  end

  @doc """
  Sends an interaction message to a user about an action performed by another user or the system.

  ## Parameters

  - recipient_id: The ID of the user to receive the message.
  - interaction_type: Atom describing the type of interaction (:new_comment, :new_order, etc.)
  - content_map: A map containing the interaction details.
    Example: %{
      text: "User X commented on your product",
      item_id: "product-123",
      comment_id: "comment-456",
      deep_link: "app://products/product-123/comments"
    }
  - sender_id: Optional ID of the user who initiated the interaction.
  - reference_id: Optional ID to associate with this message.

  ## Returns

  - {:ok, message} on success
  - {:error, changeset} on failure
  """
  def send_interaction_message(
        recipient_id,
        interaction_type,
        content_map,
        sender_id \\ nil,
        reference_id \\ nil
      ) do
    # 构建包含交互类型的内容Map
    content = [%{type: interaction_type, payload: content_map}]

    # 为交互消息创建专用的会话ID
    conversation_id = "interaction:#{recipient_id}"

    # 确保交互消息会话存在
    IO.inspect(conversation_id, label: "Conversation ID")
    ensure_system_conversation(conversation_id, "interaction", [recipient_id])

    message_params = %{
      message_id: Ecto.UUID.generate(),
      client_message_id: Ecto.UUID.generate(),
      conversation_id: conversation_id,
      sender_id: sender_id,
      receiver_id: recipient_id,
      content: content,
      message_type: :interaction,
      reference_id: reference_id,
      server_timestamp: DateTime.utc_now(),
      client_timestamp: DateTime.utc_now(),
      status: :sent
    }

    create_and_broadcast_message(message_params)
  end

  @doc """
  Sends a system announcement to a specific user.
  Note: This is a simplified version that sends to a single user.
  For full system-wide announcements, a different approach would be needed.

  ## Parameters

  - recipient_id: The ID of the user to receive the announcement.
  - content_map: A map containing the announcement content.
    Example: %{
      text: "Maintenance scheduled for tomorrow",
      title: "System Maintenance",
      deep_link: "app://news/maintenance-123"
    }
  - reference_id: Optional ID to associate with this announcement.

  ## Returns

  - {:ok, message} on success
  - {:error, changeset} on failure
  """
  def send_system_announcement_to_user(recipient_id, content_map, reference_id \\ nil) do
    # 为系统公告创建一个专用的会话ID
    conversation_id = "system:announcement:#{recipient_id}"

    # 确保系统公告会话存在
    ensure_system_conversation(conversation_id, "system_announcement", [recipient_id])

    message_params = %{
      message_id: Ecto.UUID.generate(),
      client_message_id: Ecto.UUID.generate(),
      conversation_id: conversation_id,
      sender_id: nil,
      receiver_id: recipient_id,
      content: content_map,
      message_type: :system_announcement,
      reference_id: reference_id,
      server_timestamp: DateTime.utc_now(),
      client_timestamp: DateTime.utc_now(),
      status: :sent
    }

    create_and_broadcast_message(message_params)
  end

  @doc """
  Creates a system announcement and broadcasts it to all targeted users.
  This is a more comprehensive approach for system-wide announcements.

  ## Parameters

  - content_map: A map containing the announcement content.
  - target_user_ids: List of user IDs to receive the announcement. If nil, should be sent to all users.
  - reference_id: Optional ID to associate with this announcement.

  ## Returns

  - {:ok, count} where count is the number of messages created
  - {:error, reason} on failure
  """
  def send_system_announcement(content_map, target_user_ids \\ nil, reference_id \\ nil) do
    # 如果没有指定目标用户，此处应实现获取所有有效用户ID的逻辑
    # 这里仅为示例，实际实现可能需要分批处理大量用户
    users =
      case target_user_ids do
        nil ->
          # 这里应该调用获取所有活跃用户的函数
          # 例如: FlixBackend.Accounts.list_active_users()
          []

        ids ->
          ids
      end

    # 创建批量插入的参数
    timestamp = DateTime.utc_now()

    multi =
      Enum.reduce(users, Ecto.Multi.new(), fn user_id, multi ->
        # 为系统公告创建一个专用的会话ID
        conversation_id = "system:announcement:#{user_id}"

        # 确保系统公告会话存在
        ensure_system_conversation(conversation_id, "system_announcement", [user_id])

        message_params = %{
          message_id: Ecto.UUID.generate(),
          client_message_id: Ecto.UUID.generate(),
          conversation_id: conversation_id,
          sender_id: nil,
          receiver_id: user_id,
          content: content_map,
          message_type: :system_announcement,
          reference_id: reference_id,
          server_timestamp: timestamp,
          client_timestamp: timestamp,
          status: :sent,
          inserted_at: timestamp,
          updated_at: timestamp
        }

        changeset = Message.changeset(%Message{}, message_params)
        Ecto.Multi.insert(multi, {:message, user_id}, changeset)
      end)

    # 执行批量插入
    case Repo.transaction(multi) do
      {:ok, results} ->
        # 向在线用户广播消息
        Enum.each(results, fn {{:message, user_id}, message} ->
          FlixBackendWeb.Endpoint.broadcast!(
            "user:#{user_id}",
            "new_message",
            %{message: message}
          )
        end)

        {:ok, map_size(results)}

      {:error, _failed_operation, failed_value, _changes} ->
        {:error, failed_value}
    end
  end

  @doc """
  发送私信给用户

  ## Parameters

  - sender_id: 发送者ID
  - receiver_id: 接收者ID
  - content: 消息内容
  - reference_id: 可选的关联ID

  ## Returns

  - {:ok, message} 成功
  - {:error, reason} 失败
  """
  def send_private_message(sender_id, receiver_id, content, reference_id \\ nil) do
    # 获取或创建私聊会话
    case Conversation.get_or_create_private_conversation(sender_id, receiver_id) do
      {:ok, conversation} ->
        IO.inspect(conversation.conversation_id, label: "Conversation Id")
        # 创建消息参数
        message_params = %{
          message_id: Ecto.UUID.generate(),
          client_message_id: Ecto.UUID.generate(),
          conversation_id: conversation.conversation_id,
          sender_id: sender_id,
          receiver_id: receiver_id,
          content: content,
          message_type: :private_message,
          reference_id: reference_id,
          server_timestamp: DateTime.utc_now(),
          client_timestamp: DateTime.utc_now(),
          status: :sent
        }

        # 保存消息并广播
        case create_and_broadcast_message(message_params) do
          {:ok, message} ->
            # 更新会话的最后消息信息
            update_conversation_last_message(conversation, message)

            {:ok, message}

          error ->
            error
        end

      error ->
        error
    end
  end

  # 私有方法：创建消息并广播
  defp create_and_broadcast_message(params) do
    conversation = Repo.get_by(Conversation, conversation_id: params.conversation_id)
    changeset = Message.changeset(%Message{}, params)

    case Repo.insert(changeset) do
      {:ok, message} ->
        # 获取会话信息
        if conversation do
          # 广播消息到会话所有成员
          for participant_id <- conversation.participant_ids do
            {:ok, event} = Event.create_new_message_event(message, participant_id)
            FlixBackendWeb.Endpoint.broadcast!(
              "user:#{participant_id}",
              "event",
              event
            )
          end

          # 更新会话的最后消息信息
          update_conversation_last_message(conversation, message)
        end

        {:ok, message}

      {:error, changeset} ->
        {:error, changeset}
    end
  end

  # 私有方法：更新会话的最后一条消息
  defp update_conversation_last_message(conversation, message) do
    # 获取消息预览
    preview = "[消息预览]"
    # case message.content_type do
    #   :text -> Map.get(message.content, "text", "")
    #   :image -> "[图片消息]"
    #   :audio -> "[语音消息]"
    #   :video -> "[视频消息]"
    #   :product -> "[商品]" <> Map.get(message.content, "product_name", "")
    #   :order -> "[订单]" <> Map.get(message.content, "order_id", "")
    #   :comment -> "[评论]" <> Map.get(message.content, "text", "")
    #   :like -> "[点赞]"
    #   :favorite -> "[收藏]"
    #   :system -> Map.get(message.content, "title", "系统消息")
    #   _ -> "[消息]"
    # end

    # 更新会话
    conversation
    |> Conversation.changeset(%{
      last_message_id: message.id,
      last_message_content: String.slice(preview, 0, 50),
      last_message_timestamp: message.server_timestamp,
      updated_at: DateTime.utc_now()
    })
    |> Repo.update()

    # 增加接收者的未读消息计数
    if message.receiver_id do
      case Repo.get_by(UserConversation,
             user_id: message.receiver_id,
             conversation_id: conversation.conversation_id
           ) do
        # 忽略不存在的用户会话关系
        nil -> nil
        uc -> UserConversation.increment_unread(uc)
      end
    end
  end

  # 私有方法：确保系统会话存在
  defp ensure_system_conversation(conversation_id, type, participant_ids) do
    Conversation.ensure_conversation(conversation_id, type, participant_ids)
  end

  @doc """
  获取用户的会话列表

  ## Parameters

  - user_id: 用户ID

  ## Returns

  会话列表，包括最后消息预览和未读数
  """
  def list_conversations(user_id) do
    # 查询用户的所有会话，按照更新时间降序排序
    query =
      from c in Conversation,
        join: uc in UserConversation,
        on: c.conversation_id == uc.conversation_id,
        where: uc.user_id == ^user_id,
        order_by: [desc: c.updated_at],
        select: %{
          conversation: c,
          unread_count: uc.unread_count,
          is_pinned: uc.is_pinned,
          is_muted: uc.is_muted
        }

    Repo.all(query)
  end
end
