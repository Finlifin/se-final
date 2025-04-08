defmodule FlixBackend.Messaging do
  @moduledoc """
  The Messaging context.
  Provides functions for backend services to send various types of messages.
  """

  alias FlixBackend.Data.Message
  alias FlixBackend.Repo

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
    message_params = %{
      "sender_id" => nil,
      "receiver_id" => recipient_id,
      "content" => content_map,
      "content_type" => :system,
      "message_type" => :system_notification,
      "reference_id" => reference_id
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
  def send_interaction_message(recipient_id, interaction_type, content_map, sender_id \\ nil, reference_id \\ nil) do
    # 构建包含交互类型的内容Map
    content = Map.put(content_map, "interaction_type", Atom.to_string(interaction_type))

    # 确定合适的content_type
    content_type = determine_content_type(interaction_type)

    message_params = %{
      "sender_id" => sender_id,
      "receiver_id" => recipient_id,
      "content" => content,
      "content_type" => content_type,
      "message_type" => :interaction,
      "reference_id" => reference_id
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
    message_params = %{
      "sender_id" => nil,
      "receiver_id" => recipient_id,
      "content" => content_map,
      "content_type" => :system,
      "message_type" => :system_announcement,
      "reference_id" => reference_id
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
    users = case target_user_ids do
      nil ->
        # 这里应该调用获取所有活跃用户的函数
        # 例如: FlixBackend.Accounts.list_active_users()
        []
      ids -> ids
    end

    # 创建批量插入的参数
    timestamp = DateTime.utc_now()
    multi = Enum.reduce(users, Ecto.Multi.new(), fn user_id, multi ->
      message_params = %{
        sender_id: nil,
        receiver_id: user_id,
        content: content_map,
        content_type: :system,
        message_type: :system_announcement,
        reference_id: reference_id,
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

  # 私有方法：创建消息并广播
  defp create_and_broadcast_message(params) do
    changeset = Message.changeset(%Message{}, params)

    case Repo.insert(changeset) do
      {:ok, message} ->
        # 仅当有接收者ID时才广播
        if message.receiver_id do
          FlixBackendWeb.Endpoint.broadcast!(
            "user:#{message.receiver_id}",
            "new_message",
            %{message: message}
          )
        end

        {:ok, message}

      {:error, changeset} ->
        {:error, changeset}
    end
  end

  # 私有方法：根据交互类型确定内容类型
  defp determine_content_type(interaction_type) do
    case interaction_type do
      :new_comment -> :comment
      :new_comment_reply -> :comment
      :new_order -> :order
      :like -> :like
      :favorite -> :favorite
      _ -> :system  # 默认为系统类型
    end
  end

  # 根据需求扩展更多辅助函数
end

# # 在 FlixBackend.Orders 模块中
# def create_order(order_params) do
#   # 启动事务
#   Ecto.Multi.new()
#   |> Ecto.Multi.insert(:order, Order.changeset(%Order{}, order_params))
#   |> Ecto.Multi.run(:notification, fn repo, %{order: order} ->
#     # 获取商品信息
#     product = repo.get!(Product, order.product_id)

#     # 构建消息内容
#     content = %{
#       "text" => "您收到一个新订单",
#       "title" => "新订单通知",
#       "order_id" => order.id,
#       "product_id" => order.product_id,
#       "product_name" => product.name,
#       "buyer_id" => order.buyer_id,
#       "amount" => order.amount,
#       "deep_link" => "app://orders/#{order.id}"
#     }

#     # 发送互动消息给卖家
#     FlixBackend.Messaging.send_interaction_message(
#       product.seller_id,  # 接收者是卖家
#       :new_order,         # 互动类型
#       content,            # 消息内容
#       order.buyer_id,     # 发送者是买家
#       order.id            # 关联ID是订单ID
#     )
#   end)
#   |> Repo.transaction()
# end

# # 在 FlixBackend.Comments 模块中
# def create_reply(reply_params) do
#   # 启动事务
#   Ecto.Multi.new()
#   |> Ecto.Multi.insert(:reply, Comment.changeset(%Comment{}, reply_params))
#   |> Ecto.Multi.run(:get_parent, fn repo, %{reply: reply} ->
#     # 获取原始评论
#     parent_comment = repo.get!(Comment, reply.parent_id)
#     {:ok, parent_comment}
#   end)
#   |> Ecto.Multi.run(:notification, fn _repo, %{reply: reply, get_parent: parent_comment} ->
#     # 确保不给自己发通知
#     if reply.user_id != parent_comment.user_id do
#       # 获取商品信息(假设评论关联了商品)
#       product = Repo.get!(Product, parent_comment.product_id)

#       # 构建消息内容
#       content = %{
#         "text" => "您的评论收到了新回复",
#         "title" => "评论回复",
#         "comment_id" => parent_comment.id,
#         "reply_id" => reply.id,
#         "product_id" => product.id,
#         "product_name" => product.name,
#         "deep_link" => "app://products/#{product.id}/comments/#{parent_comment.id}"
#       }

#       # 发送互动消息给原评论作者
#       FlixBackend.Messaging.send_interaction_message(
#         parent_comment.user_id, # 接收者是原评论作者
#         :new_comment_reply,     # 互动类型
#         content,                # 消息内容
#         reply.user_id,          # 发送者是回复作者
#         reply.id                # 关联ID是回复ID
#       )
#     end

#     {:ok, nil}
#   end)
#   |> Repo.transaction()
# end

# # 公告分发后台任务
# defmodule FlixBackend.Workers.AnnouncementDistribution do
#   use Oban.Worker

#   @impl Oban.Worker
#   def perform(%Oban.Job{args: %{"announcement_id" => id, "batch" => batch, "batch_size" => size}}) do
#     # 获取公告
#     announcement = FlixBackend.Announcements.get_announcement!(id)

#     # 获取当前批次的用户
#     users = FlixBackend.Accounts.get_users_batch(batch, size, announcement.target_criteria)

#     # 为每个用户创建消息并发送
#     Enum.each(users, fn user ->
#       FlixBackend.Messaging.send_system_announcement_to_user(
#         user.id,
#         announcement.content,
#         announcement.id
#       )
#     end)

#     # 如果还有更多批次，继续创建任务
#     if more_batches_needed?(batch, size, announcement.target_criteria) do
#       %{announcement_id: id, batch: batch + 1, batch_size: size}
#       |> __MODULE__.new()
#       |> Oban.insert()
#     end

#     :ok
#   end
# end

# # 启动公告分发(在公告创建后调用)
# def distribute_announcement(announcement) do
#   batch_size = 100  # 每批处理100个用户

#   # 创建第一个批次任务
#   %{announcement_id: announcement.id, batch: 1, batch_size: batch_size}
#   |> FlixBackend.Workers.AnnouncementDistribution.new()
#   |> Oban.insert()
# end
