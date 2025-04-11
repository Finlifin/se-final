defmodule FlixBackendWeb.OrderController do
  use FlixBackendWeb, :controller
  alias FlixBackend.Data.Order
  alias FlixBackend.Data.Product
  alias FlixBackend.Data.User
  alias FlixBackend.Repo
  alias FlixBackend.Guardian
  alias FlixBackendWeb.ApiResponse
  import Ecto.Query

  # 获取订单列表
  def index(conn, params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 分页参数 - 使用与message_controller一致的limit/offset方式
        limit = Map.get(params, "limit", "10") |> String.to_integer()
        offset = Map.get(params, "offset", "0") |> String.to_integer()

        # 过滤参数
        status = Map.get(params, "status")
        role = Map.get(params, "role", "buyer") # buyer或seller

        # 构建查询
        query = if role == "seller" do
          from o in Order,
          where: o.seller_id == ^user.uid,
          order_by: [desc: o.inserted_at]
        else
          from o in Order,
          where: o.buyer_id == ^user.uid,
          order_by: [desc: o.inserted_at]
        end

        # 添加状态过滤
        query = if status do
          from o in query, where: o.status == ^String.to_atom(status)
        else
          query
        end

        # 添加分页
        query = from o in query,
                limit: ^limit,
                offset: ^offset

        # 执行查询
        orders = Repo.all(query)

        # 获取总数
        count_query = if role == "seller" do
          from o in Order, where: o.seller_id == ^user.uid
        else
          from o in Order, where: o.buyer_id == ^user.uid
        end

        count_query = if status do
          from o in count_query, where: o.status == ^String.to_atom(status)
        else
          count_query
        end

        total_count = Repo.aggregate(count_query, :count, :order_id)

        # 构造响应数据
        pagination = %{
          total_count: total_count,
          limit: limit,
          offset: offset
        }

        conn
        |> put_status(:ok)
        |> json(ApiResponse.order_list_response(orders, pagination))
    end
  end

  # 获取订单详情
  def show(conn, %{"id" => order_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 查询订单，确保只能查看自己的订单（买家或卖家）
        query = from o in Order,
                where: o.order_id == ^order_id and (o.buyer_id == ^user.uid or o.seller_id == ^user.uid)

        case Repo.one(query) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("订单不存在或无权查看"))

          order ->
            # 加载关联数据
            order = order
                    |> Repo.preload(:product)
                    |> Repo.preload(:buyer)
                    |> Repo.preload(:seller)

            conn
            |> put_status(:ok)
            |> json(ApiResponse.order_response(order))
        end
    end
  end

  # 创建订单
  def create(conn, %{"product_id" => product_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 查询商品信息
        case Repo.get(Product, product_id) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("商品不存在"))

          product ->
            # 检查商品是否可购买
            if product.status != :available do
              conn
              |> put_status(:bad_request)
              |> json(ApiResponse.error_response("商品不可购买"))
            else
              # 检查是否是自己的商品
              if product.seller_id == user.uid do
                conn
                |> put_status(:bad_request)
                |> json(ApiResponse.error_response("不能购买自己的商品"))
              else
                # 创建订单
                current_time = :os.system_time(:second)
                order_params = %{
                  buyer_id: user.uid,
                  seller_id: product.seller_id,
                  product_id: product.id,
                  order_time: current_time,
                  price: product.price,
                  status: :pending
                }

                changeset = Order.changeset(%Order{}, order_params)

                case Repo.insert(changeset) do
                  {:ok, order} ->
                    # TODO: 通知卖家等

                    conn
                    |> put_status(:created)
                    |> json(ApiResponse.order_response(order, "订单创建成功"))

                  {:error, changeset} ->
                    conn
                    |> put_status(:unprocessable_entity)
                    |> json(ApiResponse.validation_error_response(changeset))
                end
              end
            end
        end
    end
  end

  # 更新订单状态
  def update_status(conn, %{"id" => order_id, "status" => status}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 检查状态值是否合法
        valid_statuses = [:pending, :payment_pending, :paid, :shipping, :completed, :cancelled, :refunded]
        new_status_atom = try do
          String.to_atom(status)
        rescue
          _ -> nil
        end

        if is_nil(new_status_atom) or !Enum.member?(valid_statuses, new_status_atom) do
          conn
          |> put_status(:bad_request)
          |> json(ApiResponse.error_response("无效的状态值"))
        else
          # 根据用户角色查询订单
          # 买家只能修改为取消（在支付前）或完成（在发货后）
          # 卖家可以修改为发货（在支付后）、取消（在发货前）或退款
          query = if status == "cancelled" || status == "completed" do
            from o in Order,
            where: o.order_id == ^order_id and (o.buyer_id == ^user.uid or o.seller_id == ^user.uid)
          else
            from o in Order,
            where: o.order_id == ^order_id and o.seller_id == ^user.uid
          end

          case Repo.one(query) do
            nil ->
              conn
              |> put_status(:not_found)
              |> json(ApiResponse.not_found_response("订单不存在或无权修改"))

            order ->
              # 检查状态变更是否合法
              if !is_valid_status_transition(order.status, new_status_atom, user.uid, order) do
                conn
                |> put_status(:bad_request)
                |> json(ApiResponse.error_response("无效的状态变更"))
              else
                # 处理订单完成的特殊情况
                result = if new_status_atom == :completed do
                  complete_order_transaction(order)
                else
                  # 更新订单状态
                  changeset = Order.changeset(order, %{status: new_status_atom})
                  Repo.update(changeset)
                end

                case result do
                  {:ok, updated_order} ->
                    # 可以添加其他逻辑，如状态变更通知等
                    conn
                    |> put_status(:ok)
                    |> json(ApiResponse.order_response(updated_order, "订单状态更新成功"))

                  {:error, _failed_operation, failed_value, _changes_so_far} ->
                    conn
                    |> put_status(:unprocessable_entity)
                    |> json(ApiResponse.error_response("处理订单失败: " <> inspect(failed_value), :error))

                  {:error, changeset} ->
                    conn
                    |> put_status(:unprocessable_entity)
                    |> json(ApiResponse.validation_error_response(changeset))
                end
              end
          end
        end
    end
  end

  # 取消订单
  def cancel(conn, %{"id" => order_id}) do
    # 调用更新状态接口，设置状态为cancelled
    update_status(conn, %{"id" => order_id, "status" => "cancelled"})
  end

  # 完成订单事务
  defp complete_order_transaction(order) do
    Repo.transaction(fn ->
      # 1. 获取买家和卖家
      buyer = Repo.get(User, order.buyer_id)
      seller = Repo.get(User, order.seller_id)
      product = Repo.get(Product, order.product_id)

      # 确保所有实体都存在
      if is_nil(buyer) or is_nil(seller) or is_nil(product) do
        Repo.rollback("买家、卖家或商品不存在")
      end

      # 2. 更新卖家余额和已售出商品列表
      # 确保product.id不重复添加到sold_product_ids中
      updated_sold_products =
        if Enum.member?(seller.sold_product_ids, product.id) do
          seller.sold_product_ids
        else
          [product.id | seller.sold_product_ids]
        end

      seller_changeset = User.changeset(seller, %{
        balance: seller.balance + order.price,
        sold_product_ids: updated_sold_products
      })

      {:ok, _updated_seller} = Repo.update(seller_changeset)

      # 3. 更新买家的已购商品列表
      # 确保product.id不重复添加到purchased_product_ids中
      updated_purchased_products =
        if Enum.member?(buyer.purchased_product_ids, product.id) do
          buyer.purchased_product_ids
        else
          [product.id | buyer.purchased_product_ids]
        end

      buyer_changeset = User.changeset(buyer, %{
        purchased_product_ids: updated_purchased_products
      })

      {:ok, _updated_buyer} = Repo.update(buyer_changeset)

      # 4. 更新商品状态为已售出
      product_changeset = Product.changeset(product, %{status: :sold})
      {:ok, _updated_product} = Repo.update(product_changeset)

      # 5. 更新订单状态
      order_changeset = Order.changeset(order, %{status: :completed})
      {:ok, updated_order} = Repo.update(order_changeset)

      # 返回更新后的订单
      updated_order
    end)
  end

  # 私有辅助函数：检查状态变更是否合法
  defp is_valid_status_transition(current_status, new_status, user_id, order) do
    cond do
      # 买家可以取消未支付的订单
      user_id == order.buyer_id && current_status in [:pending, :payment_pending] && new_status == :cancelled ->
        true

      # 买家可以确认收货完成订单
      user_id == order.buyer_id && current_status == :shipping && new_status == :completed ->
        true

      # 卖家可以发货已支付的订单
      user_id == order.seller_id && current_status == :paid && new_status == :shipping ->
        true

      # 卖家可以取消未发货的订单
      user_id == order.seller_id && current_status in [:pending, :payment_pending, :paid] && new_status == :cancelled ->
        true

      # 卖家可以退款
      user_id == order.seller_id && current_status in [:paid, :shipping] && new_status == :refunded ->
        true

      # 其他情况不允许变更
      true ->
        false
    end
  end
end
