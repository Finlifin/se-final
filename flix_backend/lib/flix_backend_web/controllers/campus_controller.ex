defmodule FlixBackendWeb.CampusController do
  use FlixBackendWeb, :controller
  alias FlixBackend.CampusService
  alias FlixBackend.Utils.Authentication
  alias FlixBackendWeb.ApiResponse

  # 获取所有校区
  def index(conn, _params) do
    case CampusService.get_all_campuses() do
      {:ok, campuses} ->
        json(conn, ApiResponse.list_response(campuses, "获取校区列表成功"))

      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 获取指定学校的校区
  def get_school_campuses(conn, %{"school_id" => school_id}) do
    case CampusService.get_school_campuses(school_id) do
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

  # 获取校区详情
  def show(conn, %{"id" => id}) do
    case CampusService.get_campus_by_id(id) do
      {:ok, campus} ->
        json(conn, ApiResponse.campus_response(campus))

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

  # 创建校区
  def create(conn, params) do
    case CampusService.create_campus(params) do
      {:ok, campus} ->
        conn
        |> put_status(:created)
        |> json(ApiResponse.campus_response(campus, "创建校区成功"))

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

  # 更新校区
  def update(conn, %{"id" => id} = params) do
    case CampusService.update_campus(id, params) do
      {:ok, campus} ->
        json(conn, ApiResponse.campus_response(campus, "更新校区成功"))

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

  # 删除校区
  def delete(conn, %{"id" => id}) do
    case CampusService.delete_campus(id) do
      {:ok, _} ->
        json(conn, ApiResponse.success_response("删除校区成功"))

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
