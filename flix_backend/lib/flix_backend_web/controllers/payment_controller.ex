defmodule FlixBackendWeb.PaymentController do
  use FlixBackendWeb, :controller
  alias FlixBackend.Data.Order
  alias FlixBackend.Data.Product
  alias FlixBackend.Data.User
  alias FlixBackend.Repo
  alias FlixBackend.Guardian
  alias FlixBackendWeb.ApiResponse
  import Ecto.Query

  # 创建支付订单
  def create_payment(conn, %{"order_id" => order_id, "payment_method" => payment_method,
                             "delivery_method" => delivery_method, "delivery_address" => delivery_address}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 查找订单并验证是否属于当前用户
        query = from o in Order,
                where: o.order_id == ^order_id and o.buyer_id == ^user.uid and o.status == :pending

        case Repo.one(query) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("订单不存在、不属于当前用户或状态不正确"))

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

                  conn
                  |> put_status(:ok)
                  |> json(ApiResponse.payment_info_response(payment_info))

                {:error, changeset} ->
                  conn
                  |> put_status(:unprocessable_entity)
                  |> json(ApiResponse.validation_error_response(changeset))
              end
            else
              conn
              |> put_status(:bad_request)
              |> json(ApiResponse.error_response("所选配送方式不可用或商品不存在"))
            end
        end
    end
  end

  # 获取支付订单状态
  def payment_status(conn, %{"order_id" => order_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        query = from o in Order,
                where: o.order_id == ^order_id and o.buyer_id == ^user.uid

        case Repo.one(query) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("订单不存在或不属于当前用户"))

          order ->
            payment_status = %{
              order_id: order.order_id,
              status: order.status,
              payment_method: order.payment_method,
              payment_time: order.payment_time,
              total_amount: order.price + (order.delivery_fee || 0)
            }

            conn
            |> put_status(:ok)
            |> json(ApiResponse.payment_status_response(payment_status))
        end
    end
  end

  # 支付成功回调
  def payment_callback(conn, %{"order_id" => order_id, "payment_status" => "success", "transaction_id" => transaction_id}) do
    # 这里应该验证支付回调的真实性，例如检查签名等

    query = from o in Order,
            where: o.order_id == ^order_id and o.status == :payment_pending

    case Repo.one(query) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response("订单不存在或状态不正确"))

      order ->
        # 更新订单状态为已支付，添加支付时间
        current_time = :os.system_time(:second)
        changeset = Order.changeset(order, %{
          status: :paid,
          payment_time: current_time
        })

        case Repo.update(changeset) do
          {:ok, _updated_order} ->
            # 这里可以添加支付成功后的业务逻辑，如通知卖家等

            conn
            |> put_status(:ok)
            |> json(ApiResponse.success_response("支付成功"))

          {:error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.validation_error_response(changeset))
        end
    end
  end

  # 获取支付方式列表
  def list_payment_methods(conn, _params) do
    payment_methods = [
      %{id: "alipay", name: "支付宝", icon: "/images/alipay.png"},
      %{id: "wechat", name: "微信支付", icon: "/images/wechat.png"},
      %{id: "card", name: "银行卡", icon: "/images/card.png"},
      %{id: "wallet", name: "钱包余额", icon: "/images/wallet.png"}
    ]

    conn
    |> put_status(:ok)
    |> json(ApiResponse.list_response(payment_methods, "获取支付方式列表成功"))
  end

  # 获取配送方式列表
  def list_delivery_methods(conn, %{"product_id" => product_id}) do
    case Repo.get(Product, product_id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response("商品不存在"))

      product ->
        delivery_methods = Enum.map(product.available_delivery_methods, fn method ->
          case method do
            "express" -> %{id: "express", name: "快递配送", icon: "/images/express.png", base_fee: 10.0}
            "pickup" -> %{id: "pickup", name: "自提", icon: "/images/pickup.png", base_fee: 0.0}
            "same_day" -> %{id: "same_day", name: "当日达", icon: "/images/same_day.png", base_fee: 15.0}
            _ -> %{id: method, name: method, icon: "/images/default.png", base_fee: 5.0}
          end
        end)

        conn
        |> put_status(:ok)
        |> json(ApiResponse.list_response(delivery_methods, "获取配送方式列表成功"))
    end
  end

  # 计算配送费用
  def calculate_delivery_fee(conn, %{"product_id" => product_id, "delivery_method" => delivery_method, "address" => address}) do
    case Repo.get(Product, product_id) do
      nil ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response("商品不存在"))

      product ->
        if Enum.member?(product.available_delivery_methods, delivery_method) do
          fee = calculate_fee(delivery_method, product)

          conn
          |> put_status(:ok)
          |> json(ApiResponse.success_response("计算配送费用成功", %{delivery_fee: fee}))
        else
          conn
          |> put_status(:bad_request)
          |> json(ApiResponse.error_response("所选配送方式不可用"))
        end
    end
  end

  # 获取用户支付历史
  def payment_history(conn, params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        limit = Map.get(params, "limit", "10") |> String.to_integer()
        offset = Map.get(params, "offset", "0") |> String.to_integer()

        query = from o in Order,
                where: o.buyer_id == ^user.uid and o.status in [:paid, :completed, :refunded],
                order_by: [desc: o.payment_time],
                limit: ^limit,
                offset: ^offset

        orders = Repo.all(query)
        total_count_query = from o in Order,
                             where: o.buyer_id == ^user.uid and o.status in [:paid, :completed, :refunded]
        total_count = Repo.aggregate(total_count_query, :count, :order_id)

        history_data = %{
          orders: orders,
          pagination: %{
            total_count: total_count,
            limit: limit,
            offset: offset
          }
        }

        conn
        |> put_status(:ok)
        |> json(ApiResponse.payment_history_response(history_data))
    end
  end

  # 取消支付
  def cancel_payment(conn, %{"order_id" => order_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        query = from o in Order,
                where: o.order_id == ^order_id and o.buyer_id == ^user.uid and o.status == :payment_pending

        case Repo.one(query) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("订单不存在、不属于当前用户或状态不正确"))

          order ->
            changeset = Order.changeset(order, %{
              status: :cancelled
            })

            case Repo.update(changeset) do
              {:ok, _updated_order} ->
                conn
                |> put_status(:ok)
                |> json(ApiResponse.success_response("支付已取消"))

              {:error, changeset} ->
                conn
                |> put_status(:unprocessable_entity)
                |> json(ApiResponse.validation_error_response(changeset))
            end
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
end
