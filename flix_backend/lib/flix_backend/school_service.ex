defmodule FlixBackend.SchoolService do
  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.School

  @doc """
  获取学校列表，支持分页

  ## 参数

  - offset: 偏移量
  - limit: 每页条数

  ## 返回值

  返回 {:ok, schools, total_count} 或 {:error, reason}
  """
  def list_schools(offset, limit) do
    query = from s in School

    # 计算总数
    total_count = Repo.aggregate(query, :count, :id)

    # 添加分页和排序
    query = from s in query,
            order_by: [asc: s.name],
            limit: ^limit,
            offset: ^((offset - 1) * limit)

    schools = Repo.all(query)

    {:ok, schools, total_count}
  end

  @doc """
  搜索学校，支持分页

  ## 参数

  - query: 搜索关键词
  - offset: 偏移量
  - limit: 每页条数

  ## 返回值

  返回 {:ok, schools, total_count} 或 {:error, reason}
  """
  def search_schools(search_query, offset, limit) when is_binary(search_query) and byte_size(search_query) > 0 do
    search_term = "%#{search_query}%"

    query = from s in School,
            where: ilike(s.name, ^search_term) or ilike(s.description, ^search_term)

    # 计算总数
    total_count = Repo.aggregate(query, :count, :id)

    # 添加分页和排序
    query = from s in query,
            order_by: [asc: s.name],
            limit: ^limit,
            offset: ^((offset - 1) * limit)

    schools = Repo.all(query)

    {:ok, schools, total_count}
  end

  def search_schools(_search_query, offset, limit) do
    # 如果搜索关键词为空，则返回所有学校
    list_schools(offset, limit)
  end

  @doc """
  获取单个学校
  """
  def get_school(id) do
    case Repo.get(School, id) do
      nil -> {:error, "School not found"}
      school -> {:ok, school}
    end
  end

  @doc """
  创建学校
  """
  def create_school(attrs) do
    %School{}
    |> School.changeset(attrs)
    |> Repo.insert()
  end

  @doc """
  更新学校
  """
  def update_school(%School{} = school, attrs) do
    school
    |> School.changeset(attrs)
    |> Repo.update()
  end

  @doc """
  删除学校
  """
  def delete_school(%School{} = school) do
    Repo.delete(school)
  end
end
