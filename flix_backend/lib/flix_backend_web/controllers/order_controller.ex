defmodule FlixBackendWeb.OrderController do
  use FlixBackendWeb, :controller
  alias FlixBackend.Data.User
  alias FlixBackend.Guardian
  alias FlixBackendWeb.ApiResponse
  alias FlixBackend.OrderService

  # 获取订单列表
  def index(conn, params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 分页参数
        limit = Map.get(params, "limit", "10") |> String.to_integer()
        offset = Map.get(params, "offset", "0") |> String.to_integer()

        # 构造服务层参数
        service_params = %{
          limit: limit,
          offset: offset,
          status: Map.get(params, "status"),
          role: Map.get(params, "role", "buyer")
        }

        # 调用服务层方法
        case OrderService.list_orders(user.uid, service_params) do
          {:ok, orders, pagination} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.order_list_response(orders, pagination))
        end
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

        # 调用服务层方法
        case OrderService.get_order(user.uid, order_id) do
          {:ok, order} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.order_response(order))

          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))
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

        # 调用服务层方法
        case OrderService.create_order(user.uid, product_id) do
          {:ok, order} ->
            conn
            |> put_status(:created)
            |> json(ApiResponse.order_response(order, "订单创建成功"))

          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))

          {:error, :bad_request, message} ->
            conn
            |> put_status(:bad_request)
            |> json(ApiResponse.error_response(message))

          {:error, :validation_error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.validation_error_response(changeset))
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

        # 调用服务层方法
        case OrderService.update_order_status(user.uid, order_id, status) do
          {:ok, updated_order} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.order_response(updated_order, "订单状态更新成功"))

          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))

          {:error, :bad_request, message} ->
            conn
            |> put_status(:bad_request)
            |> json(ApiResponse.error_response(message))

          {:error, :validation_error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.validation_error_response(changeset))

          {:error, :transaction_failed, message} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.error_response(message, :error))
        end
    end
  end

  # 取消订单
  def cancel(conn, %{"id" => order_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 调用服务层方法
        case OrderService.cancel_order(user.uid, order_id) do
          {:ok, updated_order} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.order_response(updated_order, "订单已取消"))

          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))

          {:error, :bad_request, message} ->
            conn
            |> put_status(:bad_request)
            |> json(ApiResponse.error_response(message))

          {:error, :validation_error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.validation_error_response(changeset))

          {:error, :transaction_failed, message} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.error_response(message, :error))
        end
    end
  end
end
