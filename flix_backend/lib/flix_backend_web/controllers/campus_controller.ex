defmodule FlixBackendWeb.CampusController do
  use FlixBackendWeb, :controller

  alias FlixBackend.CampusService
  alias FlixBackendWeb.ApiResponse

  action_fallback FlixBackendWeb.FallbackController

  # --- 公开 API ---

  # GET /api/v1/campuses
  def index(conn, params) do
    # 使用 offset 和 limit 进行分页
    limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1) # Ensure limit is at least 1
    offset = Map.get(params, "offset", "0") |> String.to_integer() |> max(0) # Ensure offset is non-negative
    school_id = Map.get(params, "school_id") # 可选的按学校过滤

    # 假设 CampusService.list_campuses 现在接受 offset 和 limit
    # 并返回 {:ok, campuses, total_count}
    with {:ok, campuses, total_count} <- CampusService.list_campuses(school_id, offset, limit) do
      # 在控制器中计算 currentPage 和 totalPages
      currentPage = div(offset, limit) + 1
      totalPages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

      conn
      |> put_status(:ok)
      |> json(ApiResponse.campus_list_response(campuses, total_count, currentPage, totalPages))
    end
  end

  # GET /api/v1/campuses/:id
  def show(conn, %{"id" => id}) do
    with {:ok, campus} <- CampusService.get_campus(id) do
      conn
      |> put_status(:ok)
      |> json(ApiResponse.campus_response(campus))
    end
  end

  # --- 需认证 API ---

  # PUT /api/v1/campuses/:id
  def update(conn, %{"id" => id} = campus_params) do
    with {:ok, campus} <- CampusService.get_campus(id),
         {:ok, updated_campus} <- CampusService.update_campus(campus, campus_params) do
      conn
      |> put_status(:ok)
      |> json(ApiResponse.campus_response(updated_campus, "校区更新成功"))
    end
  end

  # DELETE /api/v1/campuses/:id
  def delete(conn, %{"id" => id}) do
    with {:ok, campus} <- CampusService.get_campus(id),
         {:ok, _deleted_campus} <- CampusService.delete_campus(campus) do
      conn
      |> put_status(:ok)
      |> json(ApiResponse.success_response("校区删除成功"))
    end
  end
end
