defmodule FlixBackend.SchoolService do
  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.School
  alias FlixBackend.CampusService

  @doc """
  获取所有学校

  ## 返回值

  返回 {:ok, schools} 或 {:error, reason}
  """
  def get_schools() do
    schools = Repo.all(School)
    {:ok, schools}
  end

  @doc """
  根据ID获取学校详情

  ## 参数

  - id: 学校ID

  ## 返回值

  返回 {:ok, school} 或 {:error, reason}
  """
  def get_school_by_id(id) do
    case Repo.get(School, id) do
      nil -> {:error, :not_found, "学校不存在"}
      school -> {:ok, school}
    end
  end

  @doc """
  根据名称搜索学校

  ## 参数

  - query: 搜索关键词

  ## 返回值

  返回 {:ok, schools} 或 {:error, reason}
  """
  def search_schools(query) do
    if query && String.length(query) > 0 do
      pattern = "%#{query}%"

      schools =
        from(s in School,
          where: ilike(s.name, ^pattern) or ilike(s.code, ^pattern),
          order_by: [asc: s.name]
        )
        |> Repo.all()

      {:ok, schools}
    else
      get_schools()
    end
  end

  @doc """
  创建新学校

  ## 参数

  - attrs: 学校属性

  ## 返回值

  返回 {:ok, school} 或 {:error, reason}
  """
  def create_school(attrs) do
    %School{}
    |> School.changeset(attrs)
    |> Repo.insert()
    |> case do
      {:ok, school} -> {:ok, school}
      {:error, changeset} -> {:error, :validation_error, changeset}
    end
  end

  @doc """
  更新学校信息

  ## 参数

  - id: 学校ID
  - attrs: 要更新的属性

  ## 返回值

  返回 {:ok, school} 或 {:error, reason}
  """
  def update_school(id, attrs) do
    with {:ok, school} <- get_school_by_id(id) do
      school
      |> School.changeset(attrs)
      |> Repo.update()
      |> case do
        {:ok, updated_school} -> {:ok, updated_school}
        {:error, changeset} -> {:error, :validation_error, changeset}
      end
    end
  end

  @doc """
  删除学校

  ## 参数

  - id: 学校ID

  ## 返回值

  返回 {:ok, school} 或 {:error, reason}
  """
  def delete_school(id) do
    with {:ok, school} <- get_school_by_id(id) do
      # 检查是否有关联的校区
      case CampusService.get_school_campuses(id) do
        {:ok, campuses} when length(campuses) > 0 ->
          {:error, :constraint_error, "该学校下存在校区，无法删除"}

        _ ->
          Repo.delete(school)
      end
    end
  end

  @doc """
  获取学校的所有校区

  ## 参数

  - school_id: 学校ID

  ## 返回值

  返回 {:ok, campuses} 或 {:error, reason}
  """
  def get_school_campuses(school_id) do
    CampusService.get_school_campuses(school_id)
  end
end
