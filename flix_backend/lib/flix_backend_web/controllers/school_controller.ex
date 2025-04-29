defmodule FlixBackendWeb.SchoolController do
  use FlixBackendWeb, :controller

  alias FlixBackend.SchoolService
  alias FlixBackend.CampusService
  alias FlixBackendWeb.ApiResponse

  action_fallback FlixBackendWeb.FallbackController

  # --- 公开 API ---

  # GET /api/v1/schools
  def index(conn, params) do
    limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1)
    offset = Map.get(params, "offset", "1") |> String.to_integer() |> max(1)

    with {:ok, schools, total_count} <- SchoolService.list_schools(offset, limit) do
      currentPage = div(offset, limit) + 1
      totalPages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

      conn
      |> put_status(:ok)
      |> json(ApiResponse.school_list_response(schools, total_count, currentPage, totalPages))
    end
  end

  # GET /api/v1/schools/search?query=...
  def search(conn, %{"query" => query} = params) do
    limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1)
    offset = Map.get(params, "offset", "1") |> String.to_integer() |> max(1)

    with {:ok, schools, total_count} <- SchoolService.search_schools(query, offset, limit) do
      currentPage = div(offset, limit) + 1
      totalPages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

      conn
      |> put_status(:ok)
      |> json(ApiResponse.school_list_response(schools, total_count, currentPage, totalPages))
    end
  end

  # GET /api/v1/schools/:id
  def show(conn, %{"id" => id}) do
    with {:ok, school} <- SchoolService.get_school(id) do
      conn
      |> put_status(:ok)
      |> json(ApiResponse.school_response(school))
    end
  end

  # GET /api/v1/schools/:id/campuses
  def list_campuses(conn, %{"id" => school_id} = params) do
    limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1)
    offset = Map.get(params, "offset", "1") |> String.to_integer() |> max(1)

    with {:ok, campuses, total_count} <- CampusService.list_campuses_by_school(school_id, offset, limit) do
      currentPage = div(offset, limit) + 1
      totalPages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

      conn
      |> put_status(:ok)
      |> json(ApiResponse.campus_list_response(campuses, total_count, currentPage, totalPages))
    end
  end

  # --- 需认证 API ---

  # POST /api/v1/schools
  def create(conn, school_params) do
    with {:ok, school} <- SchoolService.create_school(school_params) do
      conn
      |> put_status(:created)
      |> put_resp_header("location", ~p"/api/v1/schools/#{school.id}")
      |> json(ApiResponse.school_response(school, "学校创建成功"))
    end
  end

  # PUT /api/v1/schools/:id
  def update(conn, %{"id" => id} = school_params) do
    with {:ok, school} <- SchoolService.get_school(id),
         {:ok, updated_school} <- SchoolService.update_school(school, school_params) do
      conn
      |> put_status(:ok)
      |> json(ApiResponse.school_response(updated_school, "学校更新成功"))
    end
  end

  # DELETE /api/v1/schools/:id
  def delete(conn, %{"id" => id}) do
    with {:ok, school} <- SchoolService.get_school(id),
         {:ok, _deleted_school} <- SchoolService.delete_school(school) do
      conn
      |> put_status(:ok)
      |> json(ApiResponse.success_response("学校删除成功"))
    end
  end

  # POST /api/v1/schools/:id/campuses
  def add_campus(conn, %{"id" => school_id} = campus_params) do
     with {:ok, _school} <- SchoolService.get_school(school_id),
          params_with_school_id <- Map.put(campus_params, "school_id", school_id),
          {:ok, campus} <- CampusService.create_campus(params_with_school_id) do
       conn
       |> put_status(:created)
       |> put_resp_header("location", ~p"/api/v1/campuses/#{campus.id}")
       |> json(ApiResponse.campus_response(campus, "校区添加成功"))
     end
  end
end
