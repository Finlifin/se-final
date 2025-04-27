defmodule FlixBackendWeb.PaymentController do
  use FlixBackendWeb, :controller
  alias FlixBackend.Data.User
  alias FlixBackend.Guardian
  alias FlixBackendWeb.ApiResponse
  alias FlixBackend.PaymentService

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

        case PaymentService.create_payment(user.uid, order_id, payment_method, delivery_method, delivery_address) do
          {:ok, payment_info} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.payment_info_response(payment_info))

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

  # 获取支付订单状态
  def payment_status(conn, %{"order_id" => order_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        case PaymentService.get_payment_status(user.uid, order_id) do
          {:ok, payment_status} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.payment_status_response(payment_status))

          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))
        end
    end
  end

  # 支付成功回调
  def payment_callback(conn, %{"order_id" => order_id, "payment_status" => "success", "transaction_id" => transaction_id}) do
    # 这里应该验证支付回调的真实性，例如检查签名等
    case PaymentService.handle_payment_callback(order_id, transaction_id) do
      {:ok, _updated_order} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("支付成功"))

      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))

      {:error, :validation_error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(ApiResponse.validation_error_response(changeset))
    end
  end

  # 获取支付方式列表
  def list_payment_methods(conn, _params) do
    {:ok, payment_methods} = PaymentService.list_payment_methods()

    conn
    |> put_status(:ok)
    |> json(ApiResponse.list_response(payment_methods, "获取支付方式列表成功"))
  end

  # 获取配送方式列表
  def list_delivery_methods(conn, %{"product_id" => product_id}) do
    case PaymentService.list_delivery_methods(product_id) do
      {:ok, delivery_methods} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.list_response(delivery_methods, "获取配送方式列表成功"))

      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))
    end
  end

  # 计算配送费用
  def calculate_delivery_fee(conn, %{"product_id" => product_id, "delivery_method" => delivery_method, "address" => address}) do
    case PaymentService.calculate_delivery_fee(product_id, delivery_method, address) do
      {:ok, result} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("计算配送费用成功", result))

      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))

      {:error, :bad_request, message} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(message))
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

        case PaymentService.get_payment_history(user.uid, limit, offset) do
          {:ok, history_data} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.payment_history_response(history_data))
        end
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

        case PaymentService.cancel_payment(user.uid, order_id) do
          {:ok, message} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.success_response(message))

          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))

          {:error, :validation_error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.validation_error_response(changeset))
        end
    end
  end
end
