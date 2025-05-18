defmodule FlixBackend.CampusService do
  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.{Campus, School}

  @doc """
  获取校区列表

  ## 参数

  - school_id: 可选的按学校过滤
  - offset: 偏移量, 默认为1
  - limit: 每页条数, 默认为10

  ## 返回值

  返回 {:ok, campuses, total_count} 或 {:error, reason}
  """
  def list_campuses(school_id \\ nil, offset \\ 1, limit \\ 10) do
    query = from(c in Campus)

    # 添加学校过滤
    query =
      if school_id do
        from c in query, where: c.school_id == ^school_id
      else
        query
      end

    # 计算总数
    total_count = Repo.aggregate(query, :count, :id)

    # 添加分页和排序
    query =
      from c in query,
        order_by: [asc: c.name],
        limit: ^limit,
        offset: ^((offset - 1) * limit)

    campuses = Repo.all(query)

    {:ok, campuses, total_count}
  end

  @doc """
  获取所有校区

  ## 返回值

  返回 {:ok, campuses} 或 {:error, reason}
  """
  def get_all_campuses() do
    campuses = Repo.all(Campus)
    {:ok, campuses}
  end

  @doc """
  获取指定学校的校区列表

  ## 参数

  - school_id: 学校ID

  ## 返回值

  返回 {:ok, campuses} 或 {:error, reason}
  """
  def get_school_campuses(school_id) do
    # 检查学校是否存在
    case Repo.get(School, school_id) do
      nil ->
        {:error, :not_found, "学校不存在"}

      _school ->
        campuses =
          from(c in Campus,
            where: c.school_id == ^school_id,
            order_by: [asc: c.name]
          )
          |> Repo.all()

        {:ok, campuses}
    end
  end

  @doc """
  获取单个校区

  ## 参数

  - id: 校区ID

  ## 返回值

  返回 {:ok, campus} 或 {:error, :not_found, message}
  """
  def get_campus_by_id(id) do
    case Repo.get(Campus, id) do
      nil -> {:error, :not_found, "校区不存在"}
      campus -> {:ok, campus}
    end
  end

  @doc """
  创建校区

  ## 参数

  - attrs: 校区属性

  ## 返回值

  返回 {:ok, campus} 或 {:error, reason}
  """
  def create_campus(attrs) do
    # 验证学校是否存在
    school_id = attrs["school_id"] || attrs[:school_id]

    case Repo.get(School, school_id) do
      nil ->
        {:error, :not_found, "所选学校不存在"}

      _school ->
        %Campus{}
        |> Campus.changeset(attrs)
        |> Repo.insert()
        |> case do
          {:ok, campus} -> {:ok, campus}
          {:error, changeset} -> {:error, :validation_error, changeset}
        end
    end
  end

  @doc """
  更新校区

  ## 参数

  - id: 校区ID
  - attrs: 要更新的属性

  ## 返回值

  返回 {:ok, campus} 或 {:error, reason}
  """
  def update_campus(id, attrs) do
    with {:ok, campus} <- get_campus_by_id(id) do
      # 如果更新包括学校ID，需验证学校是否存在
      if attrs["school_id"] || attrs[:school_id] do
        school_id = attrs["school_id"] || attrs[:school_id]

        case Repo.get(School, school_id) do
          nil ->
            {:error, :not_found, "所选学校不存在"}

          _school ->
            campus
            |> Campus.changeset(attrs)
            |> Repo.update()
            |> case do
              {:ok, updated_campus} -> {:ok, updated_campus}
              {:error, changeset} -> {:error, :validation_error, changeset}
            end
        end
      else
        campus
        |> Campus.changeset(attrs)
        |> Repo.update()
        |> case do
          {:ok, updated_campus} -> {:ok, updated_campus}
          {:error, changeset} -> {:error, :validation_error, changeset}
        end
      end
    end
  end

  @doc """
  删除校区

  ## 参数

  - id: 校区ID

  ## 返回值

  返回 {:ok, campus} 或 {:error, reason}
  """
  def delete_campus(id) do
    with {:ok, campus} <- get_campus_by_id(id) do
      Repo.delete(campus)
    end
  end
end
