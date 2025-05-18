defmodule FlixBackendWeb.MessageChannel do
  use Phoenix.Channel
  alias FlixBackend.Accounts.Account
  alias FlixBackend.Messaging
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

  # 处理同步请求 - 使用 Unix 时间戳
  def handle_in("sync", %{"last_sync_timestamp" => last_sync_timestamp}, socket) do
    user_id = socket.assigns.user_id

    timestamp =
      case last_sync_timestamp do
        ts when is_integer(ts) ->
          DateTime.from_unix!(ts, :millisecond)

        # 如果传入的是字符串，尝试转换为整数
        ts when is_binary(ts) ->
          case Integer.parse(ts) do
            {unix_timestamp, _} -> DateTime.from_unix!(unix_timestamp, :millisecond)
            :error -> DateTime.utc_now() # 默认为当前时间
          end

        _ ->
          DateTime.utc_now() # 默认为当前时间
      end

    # 获取该用户最近的消息
    messages = Messaging.get_messages_since(user_id, timestamp)

    # 确定新的同步时间戳
    new_last_sync_timestamp =
      if Enum.empty?(messages) do
        DateTime.to_unix(DateTime.utc_now(), :millisecond)
      else
        latest_message = Enum.max_by(messages, & &1.inserted_at)
        # 确保将 NaiveDateTime 转换为 DateTime 后再转为 Unix 时间戳
        case latest_message.inserted_at do
          %DateTime{} = dt ->
            DateTime.to_unix(dt, :millisecond)
          %NaiveDateTime{} = ndt ->
            ndt
            |> DateTime.from_naive!("Etc/UTC")
            |> DateTime.to_unix(:millisecond)
          _ ->
            DateTime.to_unix(DateTime.utc_now(), :millisecond)
        end
      end

    {:reply,
     {:ok,
      %{
        messages: messages,
        new_last_sync_timestamp: new_last_sync_timestamp
      }}, socket}
  end

  # 发送消息
  def handle_in("send_message", payload, socket) do
    user_id = socket.assigns.user_id

    # 确保有必要的参数
    with {:ok, content} <- Map.fetch(payload, "content"),
         {:ok, receiver_id} <- Map.fetch(payload, "receiver_id"),
         {:ok, message_id} <- Map.fetch(payload, "message_id") do

      # 构造消息内容
      message_content =
        case is_list(content) do
          true -> content  # 已经是数组格式
          false -> [%{type: "chat", text: content}]  # 转换为数组格式，默认为聊天消息
        end

      # 发送消息
      case Messaging.send_private_message(user_id, receiver_id, message_content, message_id, false) do
        {:ok, message} ->
          # 将NaiveDateTime转为Unix时间戳（毫秒）
          timestamp =
            case message.inserted_at do
              %DateTime{} = dt ->
                DateTime.to_unix(dt, :millisecond)
              %NaiveDateTime{} = ndt ->
                ndt
                |> DateTime.from_naive!("Etc/UTC")
                |> DateTime.to_unix(:millisecond)
              _ ->
                DateTime.to_unix(DateTime.utc_now(), :millisecond)
            end

          # 回复发送方确认
          {:reply,
            {:ok,
              %{
                id: message.id,
                message_id: message.message_id,
                server_timestamp: timestamp,
                status: "sent"
              }}, socket}

        {:error, reason} ->
          {:reply, {:error, %{reason: reason}}, socket}
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing required parameters (content, receiver_id, message_id)"}}, socket}
    end
  end

  # 标记消息已读
  def handle_in("mark_read", %{"message_ids" => message_ids}, socket) do
    user_id = socket.assigns.user_id

    {updated_count, _} = Messaging.mark_multiple_messages_as_read(message_ids, user_id)

    {:reply, {:ok, %{updated_count: updated_count}}, socket}
  end

  # 标记所有消息已读
  def handle_in("mark_all_read", _params, socket) do
    user_id = socket.assigns.user_id

    {updated_count, _} = Messaging.mark_all_messages_as_read(user_id)

    {:reply, {:ok, %{updated_count: updated_count}}, socket}
  end

  # 获取消息历史
  def handle_in("get_message_history", params, socket) do
    user_id = socket.assigns.user_id

    limit = Map.get(params, "limit", 20)
    offset = Map.get(params, "offset", 0)
    message_type = Map.get(params, "message_type")

    messages = Messaging.get_messages_for_user(user_id, [
      limit: limit,
      offset: offset,
      message_type: message_type
    ])

    {:reply, {:ok, %{messages: messages}}, socket}
  end

  # 获取特定时间之前的消息
  def handle_in("get_messages_before", params, socket) do
    user_id = socket.assigns.user_id

    with {:ok, timestamp} <- Map.fetch(params, "timestamp") do
      # 解析 Unix 时间戳
      datetime =
        case timestamp do
          ts when is_integer(ts) ->
            DateTime.from_unix!(ts, :millisecond)

          ts when is_binary(ts) ->
            case Integer.parse(ts) do
              {unix_timestamp, _} -> DateTime.from_unix!(unix_timestamp, :millisecond)
              :error -> DateTime.utc_now()
            end

          _ ->
            DateTime.utc_now()
        end

      limit = Map.get(params, "limit", 20)
      message_type = Map.get(params, "message_type")

      messages = Messaging.get_messages_before(user_id, datetime, [
        limit: limit,
        message_type: message_type
      ])

      {:reply, {:ok, %{messages: messages}}, socket}
    else
      :error ->
        {:reply, {:error, %{reason: "missing timestamp parameter"}}, socket}
    end
  end

  # 获取未读消息统计
  def handle_in("get_unread_stats", _params, socket) do
    user_id = socket.assigns.user_id

    stats = Messaging.get_unread_message_stats(user_id)

    {:reply, {:ok, %{stats: stats}}, socket}
  end

  # 发送系统通知
  def handle_in("send_system_notification", payload, socket) do
    with {:ok, content} <- Map.fetch(payload, "content"),
         {:ok, recipient_id} <- Map.fetch(payload, "recipient_id") do

      # 确保内容是数组格式
      message_content =
        case is_list(content) do
          true -> content
          false -> [content]
        end

      case Messaging.send_system_notification(recipient_id, message_content) do
        {:ok, message} ->
          {:reply, {:ok, %{message_id: message.id, status: "sent"}}, socket}

        {:error, reason} ->
          {:reply, {:error, %{reason: reason}}, socket}
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing required parameters"}}, socket}
    end
  end

  # 发送订单消息
  def handle_in("send_order_message", payload, socket) do
    with {:ok, content} <- Map.fetch(payload, "content"),
         {:ok, recipient_id} <- Map.fetch(payload, "recipient_id") do

      # 确保内容是数组格式
      message_content =
        case is_list(content) do
          true -> content
          false -> [content]
        end

      case Messaging.send_order_message(recipient_id, message_content) do
        {:ok, message} ->
          {:reply, {:ok, %{message_id: message.id, status: "sent"}}, socket}

        {:error, reason} ->
          {:reply, {:error, %{reason: reason}}, socket}
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing required parameters"}}, socket}
    end
  end

  # 发送支付消息
  def handle_in("send_payment_message", payload, socket) do
    with {:ok, content} <- Map.fetch(payload, "content"),
         {:ok, recipient_id} <- Map.fetch(payload, "recipient_id") do

      # 确保内容是数组格式
      message_content =
        case is_list(content) do
          true -> content
          false -> [content]
        end

      case Messaging.send_payment_message(recipient_id, message_content) do
        {:ok, message} ->
          {:reply, {:ok, %{message_id: message.id, status: "sent"}}, socket}

        {:error, reason} ->
          {:reply, {:error, %{reason: reason}}, socket}
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing required parameters"}}, socket}
    end
  end

  # 发送交互通知
  def handle_in("send_interaction_message", payload, socket) do
    user_id = socket.assigns.user_id

    with {:ok, content} <- Map.fetch(payload, "content"),
         {:ok, recipient_id} <- Map.fetch(payload, "recipient_id") do

      # 确保内容是数组格式
      message_content =
        case is_list(content) do
          true -> content
          false -> [content]
        end

      # 判断是否是自己发送的互动消息
      sender_id = Map.get(payload, "sender_id", user_id)

      # 获取客户端提供的 message_id，如果没有则生成新的 message_id
      message_id = Map.get(payload, "message_id", "#{user_id}-#{:os.system_time(:millisecond)}")

      case Messaging.send_interaction_message(recipient_id, message_content, sender_id, message_id) do
        {:ok, message} ->
          # 将NaiveDateTime转为Unix时间戳（毫秒）
          timestamp =
            case message.inserted_at do
              %DateTime{} = dt ->
                DateTime.to_unix(dt, :millisecond)
              %NaiveDateTime{} = ndt ->
                ndt
                |> DateTime.from_naive!("Etc/UTC")
                |> DateTime.to_unix(:millisecond)
              _ ->
                DateTime.to_unix(DateTime.utc_now(), :millisecond)
            end

          {:reply, {:ok, %{
            id: message.id,
            message_id: message.message_id,
            server_timestamp: timestamp,
            status: "sent"
          }}, socket}

        {:error, reason} ->
          {:reply, {:error, %{reason: reason}}, socket}
      end
    else
      :error ->
        {:reply, {:error, %{reason: "missing required parameters"}}, socket}
    end
  end
end
