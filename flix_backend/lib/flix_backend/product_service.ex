defmodule FlixBackend.ProductService do
  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.Product
  alias FlixBackend.Data.User

  @doc """
  获取产品列表，支持分页、过滤和排序。

  ## 参数

  - offset: 偏移量，从第几条数据开始获取
  - limit: 每次获取的数据条数
  - category: 可选的分类过滤
  - seller_id: 可选的卖家ID过滤
  - search_query: 可选的搜索关键词
  - min_price: 可选的最低价格过滤
  - max_price: 可选的最高价格过滤
  - sort_by: 可选的排序字段
  - sort_order: 可选的排序方向

  ## 返回值

  返回 {:ok, products, total_count} 或 {:error, reason}
  """
  def get_products(offset, limit, category \\ nil, seller_id \\ nil, search_query \\ nil,
                   min_price \\ nil, max_price \\ nil, sort_by \\ nil, sort_order \\ nil) do
    query = from p in Product,
            where: p.status == :available

    # 添加分类过滤
    query = if category do
      from p in query, where: p.category == ^category
    else
      query
    end

    # 添加卖家过滤
    query = if seller_id do
      from p in query, where: p.seller_id == ^seller_id
    else
      query
    end

    # 添加搜索过滤
    query = if search_query do
      search_term = "%#{search_query}%"
      from p in query,
        where: ilike(p.title, ^search_term) or ilike(p.description, ^search_term)
    else
      query
    end

    # 添加价格过滤
    query = if min_price do
      from p in query, where: p.price >= ^min_price
    else
      query
    end

    query = if max_price do
      from p in query, where: p.price <= ^max_price
    else
      query
    end

    # 添加排序
    query = cond do
      sort_by == "price" && sort_order == "asc" ->
        from p in query, order_by: [asc: p.price]
      sort_by == "price" && sort_order == "desc" ->
        from p in query, order_by: [desc: p.price]
      sort_by == "post_time" && sort_order == "asc" ->
        from p in query, order_by: [asc: p.post_time]
      sort_by == "post_time" && sort_order == "desc" ->
        from p in query, order_by: [desc: p.post_time]
      sort_by == "view_count" && sort_order == "desc" ->
        from p in query, order_by: [desc: p.view_count]
      true ->
        from p in query, order_by: [desc: p.post_time]
    end

    # 计算总数
    total_count = Repo.aggregate(query, :count, :id)

    # 添加分页
    query = from p in query,
            limit: ^limit,
            offset: ^offset

    products = Repo.all(query)

    {:ok, products, total_count}
  end

  @doc """
  获取单个产品详情
  """
  def get_product_by_id(id) do
    case Repo.get(Product, id) do
      nil -> {:error, "Product not found"}
      product -> {:ok, product}
    end
  end

  @doc """
  创建新产品
  """
  def create_product(attrs) do
    %Product{}
    |> Product.changeset(attrs)
    |> Repo.insert()
  end

  @doc """
  更新产品
  """
  def update_product(id, attrs, user_id) do
    with {:ok, product} <- get_product_by_id(id),
         true <- product.seller_id == user_id do
      product
      |> Product.changeset(attrs)
      |> Repo.update()
    else
      {:error, reason} -> {:error, reason}
      false -> {:error, :unauthorized}
    end
  end

  @doc """
  删除产品
  """
  def delete_product(id, user_id) do
    with {:ok, product} <- get_product_by_id(id),
         true <- product.seller_id == user_id do
      # 更新状态为deleted
      product
      |> Product.changeset(%{status: :deleted})
      |> Repo.update()
    else
      {:error, _} -> {:error, :not_found}
      false -> {:error, :unauthorized}
    end
  end

  @doc """
  收藏产品

  可能需要根据新的 User 数据模型修改此实现
  """
  def favorite_product(product_id, user_id) do
    with {:ok, _product} <- get_product_by_id(product_id),
         user = %User{} <- Repo.get(User, user_id) do

      # 检查产品是否已经在收藏列表中
      if Enum.member?(user.favorite_product_ids || [], product_id) do
        {:error, "产品已经收藏"}
      else
        # 添加产品ID到用户的收藏列表中
        favorite_product_ids = [product_id | (user.favorite_product_ids || [])]

        user
        |> User.changeset(%{favorite_product_ids: favorite_product_ids})
        |> Repo.update()
      end
    else
      nil -> {:error, "用户不存在"}
      {:error, reason} -> {:error, reason}
    end
  end

  @doc """
  取消收藏

  可能需要根据新的 User 数据模型修改此实现
  """
  def unfavorite_product(product_id, user_id) do
    with user = %User{} <- Repo.get(User, user_id) do
      if !Enum.member?(user.favorite_product_ids || [], product_id) do
        {:error, "产品未收藏"}
      else
        # 从收藏列表中移除产品ID
        favorite_product_ids =
          (user.favorite_product_ids || [])
          |> Enum.filter(fn id -> id != product_id end)

        user
        |> User.changeset(%{favorite_product_ids: favorite_product_ids})
        |> Repo.update()
      end
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  是否收藏了产品

  - product_id: 产品ID
  - user_id: 用户ID
  """
  def is_favorite_product?(product_id, user_id) do
    with user = %User{} <- Repo.get(User, user_id) do
      if Enum.member?(user.favorite_product_ids || [], product_id) do
        {:ok, true}
      else
        {:ok, false}
      end
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  获取用户收藏的产品列表

  ## 参数

  - user_id: 用户ID
  - offset: 偏移量
  - limit: 每页条数

  ## 返回值

  返回 {:ok, products, total_count} 或 {:error, reason}
  """
  def get_favorite_products(user_id, offset, limit) do
    with user = %User{} <- Repo.get(User, user_id) do
      favorite_ids = user.favorite_product_ids || []

      if Enum.empty?(favorite_ids) do
        {:ok, [], 0}
      else
        query = from p in Product,
                where: p.id in ^favorite_ids

        total_count = Repo.aggregate(query, :count, :id)

        products = query
        |> limit(^limit)
        |> offset(^offset)
        |> Repo.all()

        {:ok, products, total_count}
      end
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  增加产品查看次数
  """
  def increment_view_count(product_id) do
    with {:ok, product} <- get_product_by_id(product_id) do
      product
      |> Product.changeset(%{view_count: product.view_count + 1})
      |> Repo.update()
    end
  end
end
