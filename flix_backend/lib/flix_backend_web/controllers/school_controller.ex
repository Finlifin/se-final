defmodule FlixBackendWeb.SchoolController do
  use FlixBackendWeb, :controller
  alias FlixBackend.SchoolService
  alias FlixBackend.Utils.Authentication
  alias FlixBackendWeb.ApiResponse

  # 获取所有学校
  def index(conn, _params) do
    case SchoolService.get_schools() do
      {:ok, schools} ->
        json(conn, ApiResponse.list_response(schools, "获取学校列表成功"))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 搜索学校
  def search(conn, %{"query" => query}) do
    case SchoolService.search_schools(query) do
      {:ok, schools} ->
        json(conn, ApiResponse.list_response(schools, "搜索学校成功"))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 获取学校详情
  def show(conn, %{"id" => id}) do
    case SchoolService.get_school_by_id(id) do
      {:ok, school} ->
        json(conn, ApiResponse.school_response(school))

      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 创建学校
  def create(conn, params) do
    params = params
      |> Map.put("code", "未知代码")
    case SchoolService.create_school(params) do
      {:ok, school} ->
        conn
        |> put_status(:created)
        |> json(ApiResponse.school_response(school, "创建学校成功"))

      {:error, :validation_error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(ApiResponse.validation_error_response(changeset))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 更新学校
  def update(conn, %{"id" => id} = params) do
    case SchoolService.update_school(id, params) do
      {:ok, school} ->
        json(conn, ApiResponse.school_response(school, "更新学校成功"))

      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))

      {:error, :validation_error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(ApiResponse.validation_error_response(changeset))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 删除学校
  def delete(conn, %{"id" => id}) do
    case SchoolService.delete_school(id) do
      {:ok, _} ->
        json(conn, ApiResponse.success_response("删除学校成功"))

      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))

      {:error, :constraint_error, message} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(message))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 获取学校的所有校区
  def get_campuses(conn, %{"school_id" => school_id}) do
    case SchoolService.get_school_campuses(school_id) do
      {:ok, campuses} ->
        json(conn, ApiResponse.list_response(campuses, "获取校区列表成功"))

      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end
end
