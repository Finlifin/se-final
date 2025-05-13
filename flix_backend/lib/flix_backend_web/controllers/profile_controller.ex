defmodule FlixBackendWeb.ProfileController do
  use FlixBackendWeb, :controller

  alias FlixBackend.ProfileService
  alias FlixBackendWeb.ApiResponse # Import ApiResponse

  @doc """
  获取用户完整资料
  """
  def get_user_profile(conn, %{"userId" => user_id}) do
    case ProfileService.get_user_profile(user_id) do
      {:ok, user} ->
        json(conn, ApiResponse.success_response("获取用户资料成功", user))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(reason))
    end
  end

  @doc """
  获取商家信息，也就是user去除了敏感信息
  """
  def get_seller_profile(conn, %{"userId" => user_id}) do
    case ProfileService.get_seller_profile(user_id) do
      {:ok, seller} ->
        json(conn, ApiResponse.success_response("获取商家信息成功", seller))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(reason))
    end
  end

  @doc """
  获取用户简要信息
  """
  def get_user_abstract(conn, %{"userId" => user_id}) do
    case ProfileService.get_user_abstract(user_id) do
      {:ok, user_abstract} ->
        json(conn, ApiResponse.success_response("获取用户简要信息成功", user_abstract))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(reason))
    end
  end

  @doc """
  获取热门卖家列表
  """
  def get_popular_sellers(conn, params) do
    limit = Map.get(params, "limit", "8") |> String.to_integer()

    case ProfileService.get_popular_sellers(limit) do
      {:ok, popular_sellers} ->
        json(conn, ApiResponse.success_response("获取热门卖家成功", popular_sellers))
      {:error, reason} ->
        conn
        |> put_status(:internal_server_error)
        |> json(ApiResponse.internal_server_error_response(reason))
    end
  end

  @doc """
  更新用户资料
  """
  def update_user_profile(conn, user_params) do
    account = Guardian.Plug.current_resource(conn)
    IO.inspect(account, label: "Account")
    user_params = Map.put(user_params, "userId", account.user_id)
    case ProfileService.update_user_profile(user_params) do
      {:ok, updated_user} ->
        json(conn, ApiResponse.success_response("用户资料更新成功", updated_user))
      {:error, %Ecto.Changeset{} = changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(ApiResponse.validation_error_response(changeset))
      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  @doc """
  充值余额
  """
  def recharge_balance(conn, %{"userId" => user_id, "amount" => amount_str}) do
    account = Guardian.Plug.current_resource(conn)

    # 验证用户只能给自己充值
    if account.user_id != user_id do
      conn
      |> put_status(:forbidden)
      |> json(ApiResponse.error_response("只能给自己充值"))
    else
      case amount_str |> Integer.parse() do
        {amount, ""} when amount > 0 ->
          # 创建支付订单
          case ProfileService.create_recharge_payment(user_id, amount) do
            {:ok, payment_info} ->
              json(conn, ApiResponse.payment_info_response(payment_info))
            {:error, :not_found} ->
              conn
              |> put_status(:not_found)
              |> json(ApiResponse.not_found_response("用户不存在"))
            {:error, reason} ->
              conn
              |> put_status(:bad_request)
              |> json(ApiResponse.error_response(reason))
          end
        _ ->
          conn
          |> put_status(:bad_request)
          |> json(ApiResponse.error_response("无效的充值金额"))
      end
    end
  end

  @doc """
  更新用户头像
  """
  def update_avatar(conn, %{"userId" => user_id} = params) do
    # Add authentication check here
    avatar_url = params["avatarUrl"] || params["avatar_url"]

    case ProfileService.update_avatar(user_id, avatar_url) do
      {:ok, updated_user} ->
        json(conn, ApiResponse.success_response("头像更新成功", updated_user))
      {:error, reason} ->
        conn
        |> put_status(:bad_request) # Or internal_server_error
        |> json(ApiResponse.error_response(reason))
    end
  end

  @doc """
  获取用户发布的商品ID列表
  """
  def get_user_products(conn, %{"userId" => user_id}) do
    # Add authentication check if needed
    case ProfileService.get_user_products(user_id) do
      {:ok, product_ids} ->
        json(conn, ApiResponse.success_response("获取用户发布商品成功", product_ids))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(reason))
    end
  end

  @doc """
  获取用户已售商品ID列表
  """
  def get_user_sold_products(conn, %{"userId" => user_id}) do
     # Add authentication check if needed
    case ProfileService.get_user_sold_products(user_id) do
      {:ok, product_ids} ->
        json(conn, ApiResponse.success_response("获取用户已售商品成功", product_ids))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(reason))
    end
  end

  @doc """
  获取用户购买的商品ID列表
  """
  def get_user_purchased_products(conn, %{"userId" => user_id}) do
     # Add authentication check if needed
    case ProfileService.get_user_purchased_products(user_id) do
      {:ok, product_ids} ->
        json(conn, ApiResponse.success_response("获取用户购买商品成功", product_ids))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(reason))
    end
  end

  @doc """
  获取用户收藏的商品ID列表
  """
  def get_user_favorites(conn, %{"userId" => user_id}) do
     # Add authentication check if needed
    case ProfileService.get_user_favorites(user_id) do
      {:ok, product_ids} ->
        json(conn, ApiResponse.success_response("获取用户收藏商品成功", product_ids))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(reason))
    end
  end

  # Removed translate_error as validation errors are handled by ApiResponse.validation_error_response
end
