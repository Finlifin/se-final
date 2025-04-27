defmodule FlixBackend.PaymentService do
  import Ecto.Query
  alias FlixBackend.Repo
  alias FlixBackend.Data.{Order, Product, User}
  alias FlixBackend.Messaging

  @doc """
  创建支付订单
  """
  def create_payment(user_id, order_id, payment_method, delivery_method, delivery_address) do
    # 查找订单并验证是否属于当前用户
    query = from o in Order,
            where: o.order_id == ^order_id and o.buyer_id == ^user_id and o.status == :pending

    case Repo.one(query) do
      nil ->
        {:error, :not_found, "订单不存在、不属于当前用户或状态不正确"}

      order ->
        # 获取商品信息，验证配送方式是否可用
        product = Repo.get(Product, order.product_id)

        if product && Enum.member?(product.available_delivery_methods, delivery_method) do
          # 计算配送费用
          delivery_fee = calculate_fee(delivery_method, product)

          # 更新订单信息
          changeset = Order.changeset(order, %{
            payment_method: payment_method,
            delivery_method: delivery_method,
            delivery_address: delivery_address,
            delivery_fee: delivery_fee,
            status: :payment_pending
          })

          case Repo.update(changeset) do
            {:ok, updated_order} ->
              # 此处可以集成第三方支付系统，生成支付链接等
              payment_info = %{
                order_id: updated_order.order_id,
                amount: updated_order.price + updated_order.delivery_fee,
                payment_method: payment_method,
                payment_url: "https://example.com/pay/#{updated_order.order_id}"
              }

              # 通知卖家有新的待付款订单
              notify_seller_payment_pending(updated_order)

              {:ok, payment_info}

            {:error, changeset} ->
              {:error, :validation_error, changeset}
          end
        else
          {:error, :bad_request, "所选配送方式不可用或商品不存在"}
        end
    end
  end

  @doc """
  获取支付订单状态
  """
  def get_payment_status(user_id, order_id) do
    query = from o in Order,
            where: o.order_id == ^order_id and o.buyer_id == ^user_id

    case Repo.one(query) do
      nil ->
        {:error, :not_found, "订单不存在或不属于当前用户"}

      order ->
        payment_status = %{
          order_id: order.order_id,
          status: order.status,
          payment_method: order.payment_method,
          payment_time: order.payment_time,
          total_amount: order.price + (order.delivery_fee || 0)
        }

        {:ok, payment_status}
    end
  end

  @doc """
  支付成功回调处理
  """
  def handle_payment_callback(order_id, transaction_id) do
    # 这里应该验证支付回调的真实性，例如检查签名等

    query = from o in Order,
            where: o.order_id == ^order_id and o.status == :payment_pending

    case Repo.one(query) do
      nil ->
        {:error, :not_found, "订单不存在或状态不正确"}

      order ->
        # 更新订单状态为已支付，添加支付时间
        current_time = :os.system_time(:second)
        changeset = Order.changeset(order, %{
          status: :paid,
          payment_time: current_time
        })

        case Repo.update(changeset) do
          {:ok, updated_order} ->
            # 通知卖家订单已支付
            notify_seller_payment_success(updated_order)

            # 通知买家支付成功
            notify_buyer_payment_success(updated_order)

            {:ok, updated_order}

          {:error, changeset} ->
            {:error, :validation_error, changeset}
        end
    end
  end

  @doc """
  获取支付方式列表
  """
  def list_payment_methods do
    payment_methods = [
      %{id: "alipay", name: "支付宝", icon: "/images/alipay.png"},
      %{id: "wechat", name: "微信支付", icon: "/images/wechat.png"},
      %{id: "card", name: "银行卡", icon: "/images/card.png"},
      %{id: "wallet", name: "钱包余额", icon: "/images/wallet.png"}
    ]

    {:ok, payment_methods}
  end

  @doc """
  获取配送方式列表
  """
  def list_delivery_methods(product_id) do
    case Repo.get(Product, product_id) do
      nil ->
        {:error, :not_found, "商品不存在"}

      product ->
        delivery_methods = Enum.map(product.available_delivery_methods, fn method ->
          case method do
            "express" -> %{id: "express", name: "快递配送", icon: "/images/express.png", base_fee: 10.0}
            "pickup" -> %{id: "pickup", name: "自提", icon: "/images/pickup.png", base_fee: 0.0}
            "same_day" -> %{id: "same_day", name: "当日达", icon: "/images/same_day.png", base_fee: 15.0}
            _ -> %{id: method, name: method, icon: "/images/default.png", base_fee: 5.0}
          end
        end)

        {:ok, delivery_methods}
    end
  end

  @doc """
  计算配送费用
  """
  def calculate_delivery_fee(product_id, delivery_method, address) do
    case Repo.get(Product, product_id) do
      nil ->
        {:error, :not_found, "商品不存在"}

      product ->
        if Enum.member?(product.available_delivery_methods, delivery_method) do
          fee = calculate_fee(delivery_method, product)

          {:ok, %{delivery_fee: fee}}
        else
          {:error, :bad_request, "所选配送方式不可用"}
        end
    end
  end

  @doc """
  获取用户支付历史
  """
  def get_payment_history(user_id, limit, offset) do
    query = from o in Order,
            where: o.buyer_id == ^user_id and o.status in [:paid, :completed, :refunded],
            order_by: [desc: o.payment_time],
            limit: ^limit,
            offset: ^offset

    orders = Repo.all(query)
    total_count_query = from o in Order,
                         where: o.buyer_id == ^user_id and o.status in [:paid, :completed, :refunded]
    total_count = Repo.aggregate(total_count_query, :count, :order_id)

    history_data = %{
      orders: orders,
      pagination: %{
        total_count: total_count,
        limit: limit,
        offset: offset
      }
    }

    {:ok, history_data}
  end

  @doc """
  取消支付
  """
  def cancel_payment(user_id, order_id) do
    query = from o in Order,
            where: o.order_id == ^order_id and o.buyer_id == ^user_id and o.status == :payment_pending

    case Repo.one(query) do
      nil ->
        {:error, :not_found, "订单不存在、不属于当前用户或状态不正确"}

      order ->
        changeset = Order.changeset(order, %{
          status: :cancelled
        })

        case Repo.update(changeset) do
          {:ok, updated_order} ->
            # 通知卖家订单已取消
            notify_seller_payment_cancelled(updated_order)

            {:ok, "支付已取消"}

          {:error, changeset} ->
            {:error, :validation_error, changeset}
        end
    end
  end

  # 私有辅助函数：计算配送费用
  defp calculate_fee(delivery_method, product) do
    base_fee = case delivery_method do
      "express" -> 10.0
      "pickup" -> 0.0
      "same_day" -> 15.0
      _ -> 5.0
    end

    base_fee
  end

  # 通知卖家有待付款订单
  defp notify_seller_payment_pending(order) do
    seller = Repo.get(User, order.seller_id)
    buyer = Repo.get(User, order.buyer_id)
    product = Repo.get(Product, order.product_id)

    if seller && buyer && product do
      text_content = "#{buyer.user_name}准备购买您的商品\"#{product.title}\",订单号:#{order.order_id}"

      # 构造订单消息内容
      content = [
        %{type: "text", payload: text_content},
        %{type: "order", payload: order}
      ]

      # 发送私信
      Messaging.send_private_message(
        buyer.uid,  # 买家作为发送者
        seller.uid, # 卖家作为接收者
        content,
        order.order_id
      )
    end
  end

  # 通知卖家订单已支付成功
  defp notify_seller_payment_success(order) do
    seller = Repo.get(User, order.seller_id)
    buyer = Repo.get(User, order.buyer_id)
    product = Repo.get(Product, order.product_id)

    if seller && buyer && product do
      text_content =  "#{buyer.user_name}已支付商品\"#{product.title}\",请尽快发货,订单号:#{order.order_id}"

      # 构造订单消息内容
      content = [
        %{type: "text", payload: text_content},
        %{type: "order", payload: order}
      ]

      # 发送私信
      Messaging.send_private_message(
        buyer.uid,  # 买家作为发送者
        seller.uid, # 卖家作为接收者
        content,
        order.order_id
      )
    end
  end

  # 通知买家支付成功
  defp notify_buyer_payment_success(order) do
    buyer = Repo.get(User, order.buyer_id)
    seller = Repo.get(User, order.seller_id)
    product = Repo.get(Product, order.product_id)

    if buyer && seller && product do
      text_content = "您已成功支付商品\"#{product.title}\",订单号:#{order.order_id}"

      # 构造订单消息内容
      content = [
        %{type: "text", payload: text_content},
        %{type: "order", payload: order}
      ]

      # 发送私信
      Messaging.send_private_message(
        seller.uid, # 卖家作为发送者
        buyer.uid,  # 买家作为接收者
        content,
        order.order_id
      )
    end
  end

  # 通知卖家订单已取消
  defp notify_seller_payment_cancelled(order) do
    seller = Repo.get(User, order.seller_id)
    buyer = Repo.get(User, order.buyer_id)
    product = Repo.get(Product, order.product_id)

    if seller && buyer && product do
      text_content = "#{buyer.user_name}已取消购买商品\"#{product.title}\"的订单,订单号:#{order.order_id}"

      # 构造订单消息内容
      content = [
        %{type: "text", payload: text_content},
        %{type: "order", payload: order}
      ]

      # 发送私信
      Messaging.send_private_message(
        buyer.uid,  # 买家作为发送者
        seller.uid, # 卖家作为接收者
        content,
        order.order_id
      )
    end
  end
end
