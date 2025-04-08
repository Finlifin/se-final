defmodule FlixBackendWeb.FallbackController do
  use FlixBackendWeb, :controller
  alias FlixBackendWeb.ApiResponse

  # 处理 {:error, changeset}
  def call(conn, {:error, %Ecto.Changeset{} = changeset}) do
    conn
    |> put_status(:unprocessable_entity) # 422 Unprocessable Entity
    |> json(ApiResponse.error_response(changeset, :unprocessable_entity))
  end

  # 处理 {:error, :not_found}
  def call(conn, {:error, :not_found}) do
    conn
    |> put_status(:not_found) # 404 Not Found
    |> json(ApiResponse.error_response(:not_found, :not_found))
  end

  # 处理 {:error, :unauthorized}
  def call(conn, {:error, :unauthorized}) do
    conn
    |> put_status(:unauthorized) # 401 Unauthorized
    |> json(ApiResponse.error_response(:unauthorized, :unauthorized))
  end

   # 处理 {:error, :forbidden}
   def call(conn, {:error, :forbidden}) do
     conn
     |> put_status(:forbidden) # 403 Forbidden
     |> json(ApiResponse.error_response(:forbidden, :forbidden))
   end

  # 处理其他 {:error, reason}
  def call(conn, {:error, reason}) do
    conn
    |> put_status(:bad_request) # 400 Bad Request (默认)
    |> json(ApiResponse.error_response(reason, :bad_request))
  end
end
