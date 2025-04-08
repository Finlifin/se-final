defmodule FlixBackendWeb.MessageChannel do
  use Phoenix.Channel
  alias FlixBackend.Accounts.Account
  alias FlixBackend.Data.Message
  alias FlixBackend.Guardian

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

  # 发送私信（重命名以提高语义清晰度）
  # 客户端发送：
  # {
  #   "receiver_id": "user_uuid",
  #   "content": {
  #     "text": "消息内容",
  #     "image_urls": ["url1", "url2"],  # 可选
  #     "item_id": "product_uuid",       # 可选
  #     "title": "标题",                 # 可选
  #     "deep_link": "app://product/123" # 可选
  #   },
  #   "content_type": "text",
  #   "reference_id": "optional_id"     # 可选关联ID
  # }
  def handle_in("send_private_message", payload, socket) do
    user_id = socket.assigns.user_id

    message_params = Map.merge(payload, %{
      "sender_id" => user_id,
      "message_type" => "private_message"
    })

    IO.inspect(message_params, label: "Message Params")

    changeset = Message.changeset(%Message{}, message_params)

    case FlixBackend.Repo.insert(changeset) do
      {:ok, message} ->
        # 向接收者广播消息
        FlixBackendWeb.Endpoint.broadcast!(
          "user:#{message.receiver_id}",
          "new_message",
          %{message: message}
        )

        {:reply, {:ok, %{message: message}}, socket}

      {:error, changeset} ->
        {:reply, {:error, %{errors: changeset.errors}}, socket}
    end
  end

  # 保留原有send_message处理函数以保持向后兼容
  def handle_in("send_message", payload, socket) do
    handle_in("send_private_message", payload, socket)
  end

  # 确认单条消息已读
  def handle_in("ack_message", %{"message_id" => id}, socket) do
    user_id = socket.assigns.user_id

    case FlixBackend.Repo.get(Message, id) do
      nil ->
        {:reply, {:error, %{reason: "message not found"}}, socket}

      message ->
        if message.receiver_id == user_id do
          case Message.mark_as_read(id) do
            {:ok, _updated_message} ->
              # 通知用户的其他客户端该消息已读
              FlixBackendWeb.Endpoint.broadcast!(
                "user:#{user_id}",
                "message_status_changed",
                %{message_id: id, status: "read"}
              )

              # 更新未读消息计数
              broadcast_unread_count_update(user_id)

              {:reply, :ok, socket}

            {:error, _reason} ->
              {:reply, {:error, %{reason: "failed to mark as read"}}, socket}
          end
        else
          {:reply, {:error, %{reason: "unauthorized"}}, socket}
        end
    end
  end

  # 批量确认消息已读
  # 客户端发送：{"message_ids": ["uuid1", "uuid2", "uuid3"]}
  def handle_in("ack_messages", %{"message_ids" => ids}, socket) do
    user_id = socket.assigns.user_id

    # 验证所有消息ID为有效UUID格式
    case validate_uuids(ids) do
      :ok ->
        # 调用批量标记函数
        {count, _} = Message.mark_multiple_as_read(ids, user_id)

        if count > 0 do
          # 通知用户的其他客户端这些消息已读
          FlixBackendWeb.Endpoint.broadcast!(
            "user:#{user_id}",
            "messages_marked_read",
            %{message_ids: ids}
          )

          # 更新未读消息计数
          broadcast_unread_count_update(user_id)
        end

        {:reply, {:ok, %{status: "success", count: count}}, socket}

      {:error, invalid_ids} ->
        {:reply, {:error, %{reason: "invalid message IDs", invalid_ids: invalid_ids}}, socket}
    end
  end

  # 获取历史消息
  # 客户端发送：{
  #   "before": "ISO8601时间戳", # 可选，获取此时间之前的消息
  #   "limit": 20,              # 可选，默认20
  #   "message_type": "private_message" # 可选，消息类型过滤
  # }
  def handle_in("get_history", params, socket) do
    user_id = socket.assigns.user_id

    limit = Map.get(params, "limit", 20)
    before_time = Map.get(params, "before")
    message_type = Map.get(params, "message_type")

    query_opts = [limit: limit]

    query_opts = case message_type do
      nil -> query_opts
      type -> Keyword.put(query_opts, :message_type, String.to_atom(type))
    end

    messages = case before_time do
      nil ->
        Message.get_messages_for_user(user_id, query_opts)
      time ->
        case DateTime.from_iso8601(time) do
          {:ok, datetime, _} ->
            Message.get_messages_before(user_id, datetime, query_opts)
          {:error, _} ->
            []
        end
    end

    {:reply, {:ok, %{messages: messages}}, socket}
  end

  # 同步离线消息
  # 客户端发送：{"since": "ISO8601时间戳"}
  def handle_in("sync_messages", %{"since" => timestamp}, socket) do
    user_id = socket.assigns.user_id

    case DateTime.from_iso8601(timestamp) do
      {:ok, datetime, _} ->
        messages = Message.get_messages_since(user_id, datetime)

        {:reply, {:ok, %{
          messages: messages,
          sync_time: DateTime.utc_now() |> DateTime.to_iso8601()
        }}, socket}

      {:error, _} ->
        {:reply, {:error, %{reason: "invalid timestamp format"}}, socket}
    end
  end

  # 辅助函数：验证ID列表是否为有效UUID
  defp validate_uuids(ids) do
    invalid_ids = Enum.filter(ids, fn id ->
      case Ecto.UUID.cast(id) do
        {:ok, _} -> false
        :error -> true
      end
    end)

    if Enum.empty?(invalid_ids) do
      :ok
    else
      {:error, invalid_ids}
    end
  end

  # 辅助函数：广播未读数更新
  defp broadcast_unread_count_update(user_id) do
    # 获取最新的未读数统计
    counts = Message.count_unread_messages_by_type(user_id)

    # 通过PubSub广播最新未读数
    FlixBackendWeb.Endpoint.broadcast!(
      "user:#{user_id}",
      "unread_count_update",
      %{counts: counts}
    )

    :ok
  end
end
