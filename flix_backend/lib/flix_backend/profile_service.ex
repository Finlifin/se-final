defmodule FlixBackend.ProfileService do
  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.User

  @doc """
  获取用户完整资料
  """
  def get_user_profile(user_id) do
    case Repo.get(User, user_id) do
      nil -> {:error, "用户不存在"}
      user -> {:ok, user}
    end
  end

  @doc """
  获取商家资料（去除敏感信息）
  """
  def get_seller_profile(user_id) do
    case Repo.get(User, user_id) do
      nil -> {:error, "用户不存在"}
      user ->
        seller = %{
          uid: user.uid,
          user_name: user.user_name,
          avatar_url: user.avatar_url,
          school_id: user.school_id,
          published_product_ids: user.published_product_ids,
          current_address: user.current_address,
          phone_number: user.phone_number,
          sold_product_ids: user.sold_product_ids,
          campus_id: user.campus_id,
          sold_count: length(user.sold_product_ids || [])
        }

        {:ok, seller}
    end
  end

  @doc """
  获取用户简要信息（头像、昵称、ID等）
  """
  def get_user_abstract(user_id) do
    case Repo.get(User, user_id) do
      nil -> {:error, "用户不存在"}
      user ->
        user_abstract = %{
          uid: user.uid,
          user_name: user.user_name,
          avatar_url: user.avatar_url
        }

        {:ok, user_abstract}
    end
  end

  @doc """
  获取热门卖家
  """
  def get_popular_sellers(limit) do
    query = from u in User,
            order_by: [desc: fragment("array_length(?, 1)", u.sold_product_ids)],
            limit: ^limit

    sellers = Repo.all(query)
    |> Enum.map(fn user ->
      %{
        uid: user.uid,
        user_name: user.user_name,
        avatar_url: user.avatar_url,
        sold_count: length(user.sold_product_ids || [])
      }
    end)

    {:ok, sellers}
  end

  @doc """
  更新用户资料
  """
  def update_user_profile(user_params) do
    user_id = user_params["userId"] || user_params["user_id"]

    with %User{} = user <- Repo.get(User, user_id) do
      # 移除userId/user_id,确保不会更新ID
      attrs = case user_params do
        %{"userId" => _} -> Map.delete(user_params, "userId")
        %{"user_id" => _} -> Map.delete(user_params, "user_id")
        _ -> user_params
      end

      # 只允许更新特定字段
      allowed_attrs = Map.take(attrs, [
        "user_name", "avatar_url", "school_id", "campus_id"
      ])

      user
      |> User.changeset(allowed_attrs)
      |> Repo.update()
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  充值余额
  """
  def recharge_balance(user_id, amount) when is_integer(amount) and amount > 0 do
    with %User{} = user <- Repo.get(User, user_id) do
      user
      |> User.changeset(%{balance: user.balance + amount})
      |> Repo.update()
    else
      nil -> {:error, "用户不存在"}
    end
  end

  def recharge_balance(_user_id, _amount), do: {:error, "无效的充值金额"}

  @doc """
  更新用户头像
  """
  def update_avatar(user_id, avatar_url) when is_binary(avatar_url) do
    with %User{} = user <- Repo.get(User, user_id) do
      user
      |> User.changeset(%{avatar_url: avatar_url})
      |> Repo.update()
    else
      nil -> {:error, "用户不存在"}
    end
  end

  def update_avatar(_user_id, _avatar_url), do: {:error, "无效的头像URL"}

  @doc """
  获取用户发布的商品ID列表
  """
  def get_user_products(user_id) do
    with %User{} = user <- Repo.get(User, user_id) do
      {:ok, user.published_product_ids || []}
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  获取用户已售商品ID列表
  """
  def get_user_sold_products(user_id) do
    with %User{} = user <- Repo.get(User, user_id) do
      {:ok, user.sold_product_ids || []}
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  获取用户购买的商品ID列表
  """
  def get_user_purchased_products(user_id) do
    with %User{} = user <- Repo.get(User, user_id) do
      {:ok, user.purchased_product_ids || []}
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  获取用户收藏的商品ID列表
  """
  def get_user_favorites(user_id) do
    with %User{} = user <- Repo.get(User, user_id) do
      {:ok, user.favorite_product_ids || []}
    else
      nil -> {:error, "用户不存在"}
    end
  end

  @doc """
  获取用户列表（管理员功能）
  """
  def list_users(offset, limit) do
    query = from u in User

    # 计算总数
    total_count = Repo.aggregate(query, :count, :uid)

    # 添加分页和排序
    query = from u in query,
            order_by: [desc: u.inserted_at],
            limit: ^limit,
            offset: ^((offset - 1) * limit)

    users = Repo.all(query)

    {:ok, users, total_count}
  end
end
