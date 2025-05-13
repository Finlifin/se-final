defmodule FlixBackend.PaymentService do
  import Ecto.Query
  alias Ecto.Multi
  alias FlixBackend.Repo
  alias FlixBackend.Data.{Order, Product, User, Payment}
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
    # 使用 Ecto.Multi 处理事务
    Multi.new()
    |> Multi.run(:get_order, fn repo, _ ->
      case repo.get(Order, order_id) do
        nil -> {:error, :order_not_found}
        order ->
          if order.status == :payment_pending do
            {:ok, order}
          else
            {:error, :invalid_order_status}
          end
      end
    end)
    |> Multi.run(:process_payment, fn repo, %{get_order: order} ->
      case order.order_type do
        "product" -> process_product_payment(repo, order)
        "recharge" -> process_recharge_payment(repo, order)
        _ -> {:error, :invalid_order_type}
      end
    end)
    |> Multi.run(:update_order, fn repo, %{get_order: order, process_payment: _} ->
      order
      |> Order.changeset(%{
        status: :paid,
        payment_time: DateTime.to_unix(DateTime.utc_now()),
        transaction_id: transaction_id
      })
      |> repo.update()
    end)
    |> Repo.transaction()
    |> case do
      {:ok, %{update_order: order}} ->
        # 根据订单类型发送不同的通知
        case order.order_type do
          "product" ->
            notify_seller_payment_success(order)
            notify_buyer_payment_success(order)
          "recharge" ->
            notify_recharge_success(order)
        end
        {:ok, order}

      {:error, :get_order, :order_not_found, _} ->
        {:error, :not_found, "订单不存在"}

      {:error, :get_order, :invalid_order_status, _} ->
        {:error, :bad_request, "订单状态不正确"}

      {:error, :process_payment, :user_not_found, _} ->
        {:error, :not_found, "用户不存在"}

      {:error, _, reason, _} ->
        {:error, :internal_server_error, "处理失败: #{inspect(reason)}"}
    end
  end

  # 处理商品购买订单的支付
  defp process_product_payment(repo, order) do
    with {:ok, product} <- get_and_validate_product(repo, order.product_id),
         {:ok, updated_product} <- update_product_status(repo, product),
         {:ok, _} <- cancel_other_orders(repo, order) do
      {:ok, order}
    else
      {:error, reason} -> {:error, reason}
    end
  end

  # 处理充值订单的支付
  defp process_recharge_payment(repo, order) do
    case repo.get(User, order.buyer_id) do
      nil ->
        {:error, :user_not_found}
      user ->
        user
        |> User.changeset(%{balance: user.balance + order.price})
        |> repo.update()
        |> case do
          {:ok, _updated_user} -> {:ok, order}
          {:error, reason} -> {:error, reason}
        end
    end
  end

  # 获取并验证商品状态
  defp get_and_validate_product(repo, product_id) do
    case repo.get(Product, product_id) do
      nil ->
        {:error, :product_not_found}
      product ->
        if product.status == :available do
          {:ok, product}
        else
          {:error, :product_not_available}
        end
    end
  end

  # 更新商品状态为已售出
  defp update_product_status(repo, product) do
    product
    |> Product.changeset(%{status: :sold})
    |> repo.update()
  end

  # 取消该商品的其他订单
  defp cancel_other_orders(repo, order) do
    from(o in Order,
      where: o.product_id == ^order.product_id and o.order_id != ^order.order_id
    )
    |> repo.update_all(set: [status: :cancelled])
    {:ok, nil}
  end

  # 通知充值成功
  defp notify_recharge_success(order) do
    buyer = Repo.get(User, order.buyer_id)

    if buyer do
      text_content = "充值成功：金额￥#{order.price},订单号:#{order.order_id}"

      # 构造订单消息内容
      content = [
        %{type: "text", payload: text_content},
        %{type: "order", payload: order}
      ]

      # 发送系统通知
      Messaging.send_system_notification(
        buyer.uid, # 充值用户
        content
      )
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
            offset: ^((offset - 1) * limit)

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

  @doc """
  支付成功处理
  """
  def handle_payment_success(payment) do
    Multi.new()
    |> Multi.run(:get_order, fn repo, _ ->
      case repo.get(Order, payment.order_id) do
        nil -> {:error, :order_not_found}
        order -> {:ok, order}
      end
    end)
    |> Multi.run(:get_product, fn repo, %{get_order: order} ->
      case repo.get(Product, order.product_id) do
        nil -> {:error, :product_not_found}
        product -> {:ok, product}
      end
    end)
    |> Multi.run(:validate_product_status, fn _repo, %{get_product: product} ->
      if product.status == :available do
        {:ok, product}
      else
        {:error, :product_not_available}
      end
    end)
    |> Multi.run(:update_product, fn repo, %{get_product: product} ->
      product
      |> Product.changeset(%{status: :sold})
      |> repo.update()
    end)
    |> Multi.run(:update_order, fn repo, %{get_order: order} ->
      order
      |> Order.changeset(%{
        status: :paid,
        payment_time: DateTime.to_unix(DateTime.utc_now())
      })
      |> repo.update()
    end)
    |> Multi.run(:cancel_other_orders, fn repo, %{get_order: order} ->
      from(o in Order,
        where: o.product_id == ^order.product_id and o.order_id != ^order.order_id
      )
      |> repo.update_all(set: [status: :cancelled, cancel_reason: "商品已售出"])
      {:ok, nil}
    end)
    |> Multi.run(:update_payment, fn repo, _ ->
      payment
      |> Payment.changeset(%{status: :success})
      |> repo.update()
    end)
    |> Repo.transaction()
    |> case do
      {:ok, %{update_order: order}} ->
        # 发送支付成功通知
        notify_payment_success(order)
        {:ok, order}

      {:error, :get_order, :order_not_found, _} ->
        {:error, :order_not_found}

      {:error, :get_product, :product_not_found, _} ->
        {:error, :product_not_found}

      {:error, :validate_product_status, :product_not_available, _} ->
        {:error, :product_not_available}

      {:error, _, reason, _} ->
        {:error, reason}
    end
  end

  @doc """
  取消订单
  """
  def cancel_order(order_id, user_id, cancel_reason) do
    Multi.new()
    |> Multi.run(:get_order, fn repo, _ ->
      case repo.get(Order, order_id) do
        nil -> {:error, :order_not_found}
        order -> {:ok, order}
      end
    end)
    |> Multi.run(:validate_user, fn _repo, %{get_order: order} ->
      if order.buyer_id == user_id do
        {:ok, order}
      else
        {:error, :unauthorized}
      end
    end)
    |> Multi.run(:validate_status, fn _repo, %{get_order: order} ->
      if order.status in [:pending_payment, :pending_shipment] do
        {:ok, order}
      else
        {:error, :invalid_status}
      end
    end)
    |> Multi.run(:get_product, fn repo, %{get_order: order} ->
      case repo.get(Product, order.product_id) do
        nil -> {:error, :product_not_found}
        product -> {:ok, product}
      end
    end)
    |> Multi.run(:update_product_status, fn repo, %{get_product: product, get_order: order} ->
      # 只有在订单状态为 pending_payment 且商品状态为 :available 时才恢复商品状态
      if order.status == :pending_payment and product.status == :available do
        product
        |> Product.changeset(%{status: :available})
        |> repo.update()
      else
        {:ok, product}
      end
    end)
    |> Multi.run(:update_order, fn repo, %{get_order: order} ->
      order
      |> Order.changeset(%{
        status: :cancelled,
        # cancel_reason: cancel_reason,
        # cancel_time: DateTime.to_unix(DateTime.utc_now())
      })
      |> repo.update()
    end)
    |> Repo.transaction()
    |> case do
      {:ok, %{update_order: order}} ->
        notify_order_cancelled(order)
        {:ok, order}

      {:error, :get_order, :order_not_found, _} ->
        {:error, :order_not_found}

      {:error, :validate_user, :unauthorized, _} ->
        {:error, :unauthorized}

      {:error, :validate_status, :invalid_status, _} ->
        {:error, :invalid_status}

      {:error, _, reason, _} ->
        {:error, reason}
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

  # 通知订单取消
  defp notify_order_cancelled(order) do
    buyer = Repo.get(User, order.buyer_id)
    seller = Repo.get(User, order.seller_id)
    product = Repo.get(Product, order.product_id)

    if buyer && seller && product do
      text_content = "订单已取消: 商品\"#{product.title}\",订单号:#{order.order_id}"

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

  # 通知支付成功
  defp notify_payment_success(order) do
    buyer = Repo.get(User, order.buyer_id)
    seller = Repo.get(User, order.seller_id)
    product = Repo.get(Product, order.product_id)

    if buyer && seller && product do
      text_content = "支付成功: 商品\"#{product.title}\",订单号:#{order.order_id}"

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
end
