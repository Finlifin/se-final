defmodule FlixBackendWeb.ProductController do
  use FlixBackendWeb, :controller

  alias FlixBackend.ProductService
  alias FlixBackendWeb.ApiResponse
  alias FlixBackend.Data.User
  alias FlixBackend.Guardian

  # 获取产品列表
  def index(conn, params) do
    limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1)
    offset = Map.get(params, "offset", "0") |> String.to_integer() |> max(0)

    category = Map.get(params, "category")
    seller_id = Map.get(params, "seller_id")
    search_query = Map.get(params, "search")
    min_price = if params["min_price"], do: String.to_float(params["min_price"]), else: nil
    max_price = if params["max_price"], do: String.to_float(params["max_price"]), else: nil
    sort_by = Map.get(params, "sort_by")
    sort_order = Map.get(params, "sort_order")

    case ProductService.get_products(offset, limit, category, seller_id, search_query, min_price, max_price, sort_by, sort_order) do
      {:ok, products, total_count} ->
        products = Enum.map(products, fn product ->
          images = Map.get(product, :images, [])
          image = if length(images) > 0, do: List.first(images), else: nil
          Map.put(product, :image, image)
        end)

        currentPage = div(offset, limit) + 1
        totalPages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

        conn
        |> put_status(:ok)
        |> json(ApiResponse.product_list_response(products, total_count, currentPage, totalPages))
      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 获取单个产品详情
  def show(conn, %{"id" => id}) do
    case ProductService.get_product_by_id(id) do
      {:ok, product} ->
        ProductService.increment_view_count(id)
        conn
        |> put_status(:ok)
        |> json(ApiResponse.product_response(product))
      {:error, reason} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 创建新产品 - 兼容旧的publish接口
  def publish(conn, params) do
    create(conn, params)
  end

  # 创建新产品
  def create(conn, params) do
    account = Guardian.Plug.current_resource(conn)
    product_params = %{
      seller_id: account.user_id,
      title: params["title"],
      description: params["description"],
      price: params["price"],
      images: params["images"],
      category: params["category"],
      condition: params["condition"],
      location: params["location"],
      post_time: :os.system_time(:seconds),
      tags: params["tags"] || [],
      available_delivery_methods: params["availableDeliveryMethods"] || []
    }

    case ProductService.create_product(product_params) do
      {:ok, product} ->
        User.add_product_id(product.seller_id, product.id)

        conn
        |> put_status(:created)
        |> json(ApiResponse.product_response(product))
      {:error, changeset} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.validation_error_response(changeset))
    end
  end

  # 更新产品
  def update(conn, %{"id" => id} = params) do
    account = Guardian.Plug.current_resource(conn)

    product_params = %{}
    |> add_param_if_exists(params, "title")
    |> add_param_if_exists(params, "description")
    |> add_param_if_exists(params, "price")
    |> add_param_if_exists(params, "images")
    |> add_param_if_exists(params, "category")
    |> add_param_if_exists(params, "condition")
    |> add_param_if_exists(params, "location")
    |> add_param_if_exists(params, "status")
    |> add_param_if_exists(params, "tags")
    |> add_param_if_exists(params, "availableDeliveryMethods")

    case ProductService.update_product(id, product_params, account.user_id) do
      {:ok, product} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.product_response(product))
      {:error, :not_found} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.error_response("产品不存在"))
      {:error, :unauthorized} ->
        conn
        |> put_status(:forbidden)
        |> json(ApiResponse.error_response("没有权限修改此产品"))
      {:error, changeset} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.validation_error_response(changeset))
    end
  end

  # 删除产品
  def delete(conn, %{"id" => id}) do
    account = Guardian.Plug.current_resource(conn)

    case ProductService.delete_product(id, account.user_id) do
      {:ok, _} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("产品删除成功"))
      {:error, :not_found} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.error_response("产品不存在"))
      {:error, :unauthorized} ->
        conn
        |> put_status(:forbidden)
        |> json(ApiResponse.error_response("没有权限删除此产品"))
    end
  end

  # 收藏产品
  def favorite(conn, %{"id" => id}) do
    account = Guardian.Plug.current_resource(conn)

    case ProductService.favorite_product(id, account.user_id) do
      {:ok, _} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("产品收藏成功"))
      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 取消收藏
  def unfavorite(conn, %{"id" => id}) do
    account = Guardian.Plug.current_resource(conn)

    case ProductService.unfavorite_product(id, account.user_id) do
      {:ok, _} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("取消收藏成功"))
      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 是否收藏了某件商品
  def is_favorite(conn, %{"id" => id}) do
    account = Guardian.Plug.current_resource(conn)

    case ProductService.is_favorite_product?(id, account.user_id) do
      {:ok, is_favorite} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("查询成功", is_favorite))
      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 获取收藏的产品
  def favorites(conn, params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      current_user ->
        limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1)
        offset = Map.get(params, "offset", "0") |> String.to_integer() |> max(0)

        case ProductService.get_favorite_products(current_user.user_id, offset, limit) do
          {:ok, products, total_count} ->
            currentPage = div(offset, limit) + 1
            totalPages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

            conn
            |> put_status(:ok)
            |> json(ApiResponse.product_list_response(products, total_count, currentPage, totalPages))
          {:error, reason} ->
            conn
            |> put_status(:bad_request)
            |> json(ApiResponse.error_response(reason))
        end
    end
  end

  # 辅助函数：有条件地添加参数
  defp add_param_if_exists(map, params, key) do
    case Map.get(params, key) do
      nil -> map
      value -> Map.put(map, String.to_atom(key), value)
    end
  end
end
