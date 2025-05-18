defmodule FlixBackend.ProductService do
  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.Product
  alias FlixBackend.Data.User
  alias FlixBackend.Messaging

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
  - available_status: 商品状态筛选
  - campus_id: 可选的校区ID过滤

  ## 返回值

  返回 {:ok, products, total_count} 或 {:error, reason}
  """
  def get_products(
        offset,
        limit,
        category \\ nil,
        seller_id \\ nil,
        search_query \\ nil,
        min_price \\ nil,
        max_price \\ nil,
        sort_by \\ nil,
        sort_order \\ nil,
        available_status \\ [:available, :sold],
        campus_id \\ nil
      ) do
    # 基础查询，排除 search_vector 字段
    query =
      from p in Product,
        where: p.status in ^available_status,
        select:
          map(p, [
            :id,
            :seller_id,
            :title,
            :description,
            :price,
            :images,
            :category,
            :condition,
            :location,
            :post_time,
            :status,
            :view_count,
            :favorite_count,
            :tags,
            :available_delivery_methods,
            :campus_id,
            :inserted_at,
            :updated_at
          ])

    # 添加分类过滤
    query =
      if category do
        from p in query, where: p.category == ^category
      else
        query
      end

    # 添加卖家过滤
    query =
      if seller_id do
        from p in query, where: p.seller_id == ^seller_id
      else
        query
      end

    # 添加校区过滤
    query =
      if campus_id do
        from p in query, where: p.campus_id == ^campus_id
      else
        query
      end

    # 添加搜索过滤 - 使用全文搜索结合模糊匹配
    processed_query =
      if search_query && search_query != "" do
        # 准备搜索词，移除特殊字符并准备为tsquery格式
        search_query
        |> String.replace(~r/[^\p{L}\p{N}_\s]/u, "")
        |> String.split(~r/\s+/)
        |> Enum.filter(&(String.length(&1) > 0))
        |> Enum.map(&(&1 <> ":*"))
        |> Enum.join(" & ")
      else
        ""
      end

    query =
      if search_query && search_query != "" do
        # 准备模糊查询的模式字符串
        fuzzy_pattern = "%#{search_query}%"

        if processed_query != "" do
          # 使用全文搜索结合模糊匹配
          from p in Product,
            where:
              p.status in ^available_status and
                (fragment("search_vector @@ to_tsquery('simple', ?)", ^processed_query) or
                   ilike(p.title, ^fuzzy_pattern) or
                   ilike(p.description, ^fuzzy_pattern)),
            order_by: [
              desc: fragment("ts_rank(search_vector, to_tsquery('simple', ?))", ^processed_query)
            ],
            select:
              map(p, [
                :id,
                :seller_id,
                :title,
                :description,
                :price,
                :images,
                :category,
                :condition,
                :location,
                :post_time,
                :status,
                :view_count,
                :favorite_count,
                :tags,
                :available_delivery_methods,
                :campus_id,
                :inserted_at,
                :updated_at
              ])
        else
          # 如果处理后的查询为空，仅使用模糊匹配
          from p in query,
            where: ilike(p.title, ^fuzzy_pattern) or ilike(p.description, ^fuzzy_pattern)
        end
      else
        query
      end

    # 添加价格过滤
    query =
      if min_price do
        from p in query, where: p.price >= ^min_price
      else
        query
      end

    query =
      if max_price do
        from p in query, where: p.price <= ^max_price
      else
        query
      end

    # 添加排序
    query =
      if search_query && String.length(search_query) > 0 && processed_query != "" do
        # 全文搜索结果的排序
        base_query =
          from p in query,
            order_by: [
              desc: fragment("ts_rank(search_vector, to_tsquery('simple', ?))", ^processed_query)
            ]

        # 在保持相关性排序的基础上应用用户请求的排序
        cond do
          sort_by == "price" && sort_order == "asc" ->
            from p in base_query, order_by: [asc: p.price, asc: p.id]

          sort_by == "price" && sort_order == "desc" ->
            from p in base_query, order_by: [desc: p.price, asc: p.id]

          sort_by == "post_time" && sort_order == "asc" ->
            from p in base_query, order_by: [asc: p.post_time, asc: p.id]

          sort_by == "post_time" && sort_order == "desc" ->
            from p in base_query, order_by: [desc: p.post_time, asc: p.id]

          sort_by == "view_count" && sort_order == "desc" ->
            from p in base_query, order_by: [desc: p.view_count, asc: p.id]

          true ->
            # 默认排序，保持相关性为主要排序条件，添加ID作为二级排序
            from p in base_query, order_by: [asc: p.id]
        end
      else
        # 非搜索情况下的排序
        cond do
          sort_by == "price" && sort_order == "asc" ->
            from p in query, order_by: [asc: p.price, asc: p.id]

          sort_by == "price" && sort_order == "desc" ->
            from p in query, order_by: [desc: p.price, asc: p.id]

          sort_by == "post_time" && sort_order == "asc" ->
            from p in query, order_by: [asc: p.post_time, asc: p.id]

          sort_by == "post_time" && sort_order == "desc" ->
            from p in query, order_by: [desc: p.post_time, asc: p.id]

          sort_by == "view_count" && sort_order == "desc" ->
            from p in query, order_by: [desc: p.view_count, asc: p.id]

          true ->
            # 默认按发布时间降序排序，并添加ID作为二级排序
            from p in query, order_by: [desc: p.post_time, asc: p.id]
        end
      end

    # 计算总数
    count_query =
      query
      |> exclude(:order_by)
      |> exclude(:select)
      |> select([p], count(p.id))

    total_count = Repo.one(count_query)

    # 添加分页
    offset = (offset - 1) * limit

    query =
      from p in query,
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
      nil -> {:error, :not_found, "商品不存在"}
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
    |> case do
      {:ok, product} -> {:ok, product}
      {:error, changeset} -> {:error, :validation_error, changeset}
    end
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
      |> case do
        {:ok, updated_product} -> {:ok, updated_product}
        {:error, changeset} -> {:error, :validation_error, changeset}
      end
    else
      {:error, :not_found, message} -> {:error, :not_found, message}
      false -> {:error, :unauthorized, "无权更新此商品"}
    end
  end

  @doc """
  更改商品状态
  """
  def update_product_status(id, status, user_id) do
    with {:ok, product} <- get_product_by_id(id),
         true <- product.seller_id == user_id do
      product
      |> Product.changeset(%{status: status})
      |> Repo.update()
      |> case do
        {:ok, updated_product} ->
          # 发送商品状态变更通知给关注此商品的用户
          send_product_status_change_notifications(updated_product)
          {:ok, updated_product}

        {:error, changeset} ->
          {:error, :validation_error, changeset}
      end
    else
      {:error, :not_found, message} -> {:error, :not_found, message}
      false -> {:error, :unauthorized, "无权更新此商品状态"}
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
      |> case do
        {:ok, updated_product} -> {:ok, updated_product}
        {:error, changeset} -> {:error, :validation_error, changeset}
      end
    else
      {:error, :not_found, message} -> {:error, :not_found, message}
      false -> {:error, :unauthorized, "无权删除此商品"}
    end
  end

  @doc """
  收藏产品
  """
  def favorite_product(product_id, user_id) do
    with {:ok, product} <- get_product_by_id(product_id),
         user = %User{} <- Repo.get(User, user_id) do
      # 检查产品是否已经在收藏列表中
      if Enum.member?(user.favorite_product_ids || [], product_id) do
        {:error, :bad_request, "产品已经收藏"}
      else
        # 添加产品ID到用户的收藏列表中
        favorite_product_ids = [product_id | user.favorite_product_ids || []]

        result =
          user
          |> User.changeset(%{favorite_product_ids: favorite_product_ids})
          |> Repo.update()

        case result do
          {:ok, updated_user} ->
            # 发送通知给商品卖家
            notify_seller_of_favorite(product, updated_user)
            {:ok, updated_user}

          {:error, changeset} ->
            {:error, :validation_error, changeset}
        end
      end
    else
      nil -> {:error, :not_found, "用户不存在"}
      {:error, :not_found, message} -> {:error, :not_found, message}
    end
  end

  @doc """
  取消收藏
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
        query =
          from p in Product,
            where: p.id in ^favorite_ids,
            # 添加明确的排序规则，按更新时间降序，ID升序确保分页结果一致性
            order_by: [desc: p.updated_at, asc: p.id]

        total_count = Repo.aggregate(query, :count, :id)

        products =
          query
          |> limit(^limit)
          |> offset(^((offset - 1) * limit))
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

  # 通知卖家有人收藏了他的商品
  defp notify_seller_of_favorite(product, user) do
    seller = Repo.get(User, product.seller_id)

    if seller do
      text_content = "我收藏了你的商品\"#{product.title}\""

      # 构造收藏消息内容
      content = [
        %{type: "text", payload: text_content},
        %{type: "product", payload: product}
      ]

      # 发送私信
      Messaging.send_private_message(
        # 收藏的用户作为发送者
        user.uid,
        # 卖家作为接收者
        seller.uid,
        content,
        "#{user.uid}-#{DateTime.utc_now() |> DateTime.to_unix(:millisecond)}"
      )
    end
  end

  # 发送商品状态变更通知给关注此商品的用户
  defp send_product_status_change_notifications(product) do
    # 查找所有收藏了此商品的用户
    query =
      from u in User,
        where: fragment("? = ANY(favorite_product_ids)", ^product.id)

    users = Repo.all(query)

    # 获取卖家信息
    seller = Repo.get(User, product.seller_id)

    if !seller do
      # 如果找不到卖家信息，直接返回
      nil
    else
      # 根据新状态生成通知内容
      {title, text} =
        case product.status do
          :available -> {"商品已上架", "您收藏的商品\"#{product.title}\"已上架"}
          :unavailable -> {"商品已下架", "您收藏的商品\"#{product.title}\"已下架"}
          :reserved -> {"商品已被预定", "您收藏的商品\"#{product.title}\"已被预定"}
          :sold -> {"商品已售出", "您收藏的商品\"#{product.title}\"已售出"}
          _ -> {"商品状态已更新", "您收藏的商品\"#{product.title}\"状态已更新"}
        end

      # 向每个用户发送通知
      Enum.each(users, fn user ->
        # 构造商品状态更新消息内容
        content = [
          %{type: "text", payload: text},
          %{type: "product", payload: product}
        ]

        # 发送私信
        Messaging.send_private_message(
          # 卖家作为发送者
          seller.uid,
          # 收藏的用户作为接收者
          user.uid,
          content,
          product.id
        )
      end)
    end
  end
end
