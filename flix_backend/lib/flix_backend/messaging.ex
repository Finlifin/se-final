defmodule FlixBackend.Messaging do
  @moduledoc """
  消息系统上下文模块。

  提供了完整的消息发送和管理功能，是后端服务与消息系统交互的主要接口。

  ## 消息分类

  消息系统支持以下几类消息：

  1. 系统消息（:system）
     - 系统通知：如账户验证、功能提醒等
     - 系统公告：如维护通知、新功能发布等

  2. 交互通知（:notification）
     - 用户互动：如评论、点赞等
     - 其他通知类消息

  3. 聊天消息（:chat）
     - 用户之间的私信

  4. 订单消息（:order）
     - 订单状态更新
     - 订单相关通知

  5. 支付消息（:payment）
     - 支付状态更新
     - 交易通知

  ## 服务器消息与用户消息

  - 服务器消息：sender 为 nil
  - 用户消息：sender 为发送用户的 ID

  所有非用户发送的消息均为服务器消息。

  ## 消息ID格式

  - 用户发送消息: ${sender_phone_number}-${client_message_uuid}
  - 服务器发送消息: server-${uuid}
  """

  alias FlixBackend.Data.Message
  alias FlixBackend.Repo
  alias FlixBackend.Accounts.Account

  # 获取用户电话号码的函数（如果系统中有的话）
  defp get_user_phone(user_id) do
    case Account.get_account_by_user_id(user_id) do
      %{phone_number: phone} when is_binary(phone) -> phone
      _ -> user_id # 如果无法获取电话，使用用户ID作为默认值
    end
  end

  # 生成服务器消息ID
  defp generate_server_message_id do
    "server-#{Ecto.UUID.generate()}"
  end

  @doc """
  发送系统通知给指定用户。

  ## 参数

  - recipient_id: 接收者用户ID
  - content: 通知内容数组，每项都是一个消息内容对象
    例如: [%{
      type: "notification",
      title: "账户验证",
      text: "您的账户已验证",
      deep_link: "app://settings/account",
      reference_id: "ref-123"
    }]

  ## 返回

  - {:ok, message} 成功
  - {:error, changeset} 失败
  """
  def send_system_notification(recipient_id, content) do
    # 生成消息参数
    message_params = %{
      message_id: generate_server_message_id(),
      sender: nil, # 服务器发送的消息
      receiver: recipient_id,
      content: content,
      message_type: :system,
      read: false
    }

    create_message(message_params)
  end

  @doc """
  发送交互通知给指定用户。

  ## 参数

  - recipient_id: 接收者用户ID
  - content: 通知内容数组，每项都是一个消息内容对象
    例如: [%{
      type: "interaction",
      interaction_type: :new_comment,
      text: "用户X评论了您的商品",
      payload: %{item_id: "product-123", comment_id: "comment-456"},
      reference_id: "ref-789"
    }]
  - sender_id: 发送者ID，如果为空则为系统发送
  - message_id: 消息唯一标识符，格式为 ${sender_phone_number | server}-${client_message_uuid}，如果为空则自动生成

  ## 返回

  - {:ok, message} 成功
  - {:error, changeset} 失败
  """
  def send_interaction_message(recipient_id, content, sender_id \\ nil, message_id \\ nil) do
    # 如果没有提供message_id，则生成一个
    message_id =
      cond do
        message_id != nil ->
          message_id
        sender_id != nil ->
          sender_identifier = get_user_phone(sender_id)
          "#{sender_identifier}-#{Ecto.UUID.generate()}"
        true ->
          generate_server_message_id()
      end

    message_params = %{
      message_id: message_id,
      sender: sender_id, # 如果sender_id为空，则为服务器发送的
      receiver: recipient_id,
      content: content,
      message_type: :notification,
      read: false
    }

    create_message(message_params)
  end

  @doc """
  发送系统公告给指定用户。

  ## 参数

  - recipient_id: 接收者用户ID
  - content: 公告内容数组，每项都是一个消息内容对象
    例如: [%{
      type: "announcement",
      title: "系统维护",
      text: "系统将于明天进行维护",
      deep_link: "app://news/maintenance-123",
      reference_id: "ref-123"
    }]

  ## 返回

  - {:ok, message} 成功
  - {:error, changeset} 失败
  """
  def send_system_announcement_to_user(recipient_id, content) do
    message_params = %{
      message_id: generate_server_message_id(),
      sender: nil, # 服务器发送的消息
      receiver: recipient_id,
      content: content,
      message_type: :system,
      read: false
    }

    create_message(message_params)
  end

  @doc """
  创建系统公告并广播给所有目标用户。
  这是一个更全面的系统公告发送方法。

  ## 参数

  - content: 公告内容数组，每项都是一个消息内容对象
  - target_user_ids: 接收公告的用户ID列表。如果为nil，应发送给所有用户。

  ## 返回

  - {:ok, count} 成功，count是创建的消息数量
  - {:error, reason} 失败
  """
  def send_system_announcement(content, target_user_ids \\ nil) do
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
    base_message_id = generate_server_message_id()

    multi =
      Enum.reduce(users, Ecto.Multi.new(), fn user_id, multi ->
        # 为每个用户生成唯一的message_id
        user_specific_id = "#{base_message_id}-#{user_id}"

        message_params = %{
          message_id: user_specific_id,
          sender: nil, # 服务器发送的消息
          receiver: user_id,
          content: content,
          message_type: :system,
          read: false,
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

  ## 参数

  - sender_id: 发送者ID
  - receiver_id: 接收者ID
  - content: 消息内容数组，每项都是一个消息内容对象
    例如: [%{
      type: "chat",
      text: "你好，这是一条私信",
      reference_id: "ref-123"
    }]
  - message_id: 消息唯一标识符，格式为 ${sender_phone_number | server}-${client_message_uuid}

  ## 返回

  - {:ok, message} 成功
  - {:error, reason} 失败
  """
  def send_private_message(sender_id, receiver_id, content, message_id, for_two \\ true) do
    # 创建消息参数
    message_params = %{
      message_id: message_id,
      sender: sender_id,
      receiver: receiver_id,
      content: content,
      message_type: :chat,
      read: false
    }

# 保存消息并广播
    create_message(message_params, for_two)
  end

  @doc """
  发送订单相关消息给用户

  ## 参数

  - recipient_id: 接收者ID
  - content: 消息内容数组，每项都是一个消息内容对象
    例如: [%{
      type: "order",
      order_id: "order-123",
      title: "订单状态更新",
      text: "您的订单已发货",
      status: "shipped",
      deep_link: "app://orders/order-123",
      reference_id: "ref-123"
    }]

  ## 返回

  - {:ok, message} 成功
  - {:error, reason} 失败
  """
  def send_order_message(message_id, recipient_id, content) do
    message_params = %{
      message_id: message_id,
      sender: nil, # 服务器发送的消息
      receiver: recipient_id,
      content: content,
      message_type: :order,
      read: false
    }

    create_message(message_params)
  end

  @doc """
  发送支付相关消息给用户

  ## 参数

  - recipient_id: 接收者ID
  - content: 消息内容数组，每项都是一个消息内容对象
    例如: [%{
      type: "payment",
      payment_id: "payment-123",
      title: "支付成功",
      text: "您已成功支付￥100",
      status: "success",
      amount: 10000,
      deep_link: "app://payments/payment-123",
      reference_id: "ref-123"
    }]

  ## 返回

  - {:ok, message} 成功
  - {:error, reason} 失败
  """
  def send_payment_message(recipient_id, content) do
    message_params = %{
      message_id: generate_server_message_id(),
      sender: nil, # 服务器发送的消息
      receiver: recipient_id,
      content: content,
      message_type: :payment,
      read: false
    }

    create_message(message_params)
  end

  # 创建消息并广播
  defp create_message(params, for_two \\ true) do
    changeset = Message.changeset(%Message{}, params)

    case Repo.insert(changeset) do
      {:ok, message} ->
        # 广播消息到接收者频道
        if message.receiver do
          FlixBackendWeb.Endpoint.broadcast!(
            "user:#{message.receiver}",
            "new_message",
            %{message: message}
          )
        end

        if for_two and message.sender do
          FlixBackendWeb.Endpoint.broadcast!(
            "user:#{message.sender}",
            "new_message",
            %{message: message}
          )
        end

        {:ok, message}

      error ->
        error
    end
  end

  @doc """
  获取用户未读消息统计

  ## Parameters

  - user_id: 用户ID

  ## Returns

  %{total: 总未读数, system: 系统消息数, notification: 通知数, chat: 聊天消息数, ...}
  """
  def get_unread_message_stats(user_id) do
    Message.count_unread_messages_by_type(user_id)
  end

  @doc """
  标记消息为已读

  ## Parameters

  - message_id: 消息ID

  ## Returns

  - {:ok, message} 成功
  - {:error, reason} 失败
  """
  def mark_message_as_read(message_id) do
    Message.mark_as_read(message_id)
  end

  @doc """
  标记用户所有消息为已读

  ## Parameters

  - user_id: 用户ID

  ## Returns

  - {updated_count, nil} 成功，返回更新的消息数量
  """
  def mark_all_messages_as_read(user_id) do
    Message.mark_all_as_read(user_id)
  end

  @doc """
  批量标记消息为已读

  ## Parameters

  - message_ids: 消息ID列表
  - user_id: 用户ID

  ## Returns

  - {updated_count, nil} 成功，返回更新的消息数量
  """
  def mark_multiple_messages_as_read(message_ids, user_id) do
    Message.mark_multiple_as_read(message_ids, user_id)
  end

  @doc """
  获取用户消息列表

  ## Parameters

  - user_id: 用户ID
  - opts: 选项 (limit: 限制数量, offset: 偏移量, message_type: 消息类型)

  ## Returns

  消息列表
  """
  def get_messages_for_user(user_id, opts \\ []) do
    Message.get_messages_for_user(user_id, opts)
  end

  @doc """
  获取指定时间前的消息

  ## Parameters

  - user_id: 用户ID
  - datetime: 时间点
  - opts: 选项 (limit: 限制数量, message_type: 消息类型)

  ## Returns

  消息列表
  """
  def get_messages_before(user_id, datetime, opts \\ []) do
    Message.get_messages_before(user_id, datetime, opts)
  end

  @doc """
  获取指定时间后的消息（用于同步）

  ## Parameters

  - user_id: 用户ID
  - timestamp: 时间戳
  - opts: 选项 (limit: 限制数量)

  ## Returns

  消息列表
  """
  def get_messages_since(user_id, timestamp, opts \\ []) do
    Message.get_messages_since(user_id, timestamp, opts)
  end
end
