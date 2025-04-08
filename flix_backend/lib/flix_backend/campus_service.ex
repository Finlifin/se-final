defmodule FlixBackend.CampusService do
  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.Campus

  @doc """
  获取校区列表

  ## 参数

  - school_id: 可选的按学校过滤
  - offset: 偏移量
  - limit: 每页条数

  ## 返回值

  返回 {:ok, campuses, total_count} 或 {:error, reason}
  """
  def list_campuses(school_id \\ nil, offset, limit) do
    query = from c in Campus

    # 添加学校过滤
    query = if school_id do
      from c in query, where: c.school_id == ^school_id
    else
      query
    end

    # 计算总数
    total_count = Repo.aggregate(query, :count, :id)

    # 添加分页和排序
    query = from c in query,
            order_by: [asc: c.name],
            limit: ^limit,
            offset: ^offset

    campuses = Repo.all(query)

    {:ok, campuses, total_count}
  end

  @doc """
  获取指定学校的校区列表

  ## 参数

  - school_id: 学校ID
  - offset: 偏移量
  - limit: 每页条数

  ## 返回值

  返回 {:ok, campuses, total_count} 或 {:error, reason}
  """
  def list_campuses_by_school(school_id, offset, limit) do
    list_campuses(school_id, offset, limit)
  end

  @doc """
  获取单个校区
  """
  def get_campus(id) do
    case Repo.get(Campus, id) do
      nil -> {:error, "Campus not found"}
      campus -> {:ok, campus}
    end
  end

  @doc """
  创建校区
  """
  def create_campus(attrs) do
    %Campus{}
    |> Campus.changeset(attrs)
    |> Repo.insert()
  end

  @doc """
  更新校区
  """
  def update_campus(%Campus{} = campus, attrs) do
    campus
    |> Campus.changeset(attrs)
    |> Repo.update()
  end

  @doc """
  删除校区
  """
  def delete_campus(%Campus{} = campus) do
    Repo.delete(campus)
  end
end
