defmodule FlixBackendWeb.FallbackController do
  use FlixBackendWeb, :controller

  def call(conn, {:error, %Ecto.Changeset{} = changeset}) do
    conn
    |> put_status(:unprocessable_entity)
    |> put_view(FlixBackendWeb.ChangesetView)
    |> render("error.json", changeset: changeset)
  end

  def call(conn, {:error, :not_found}) do
    conn
    |> put_status(:not_found)
    |> put_view(FlixBackendWeb.ErrorView)
    |> render("404.json")
  end

  def call(conn, {:error, :unauthorized}) do
    conn
    |> put_status(:unauthorized)
    |> put_view(FlixBackendWeb.ErrorView)
    |> render("401.json")
  end

  def call(conn, {:error, reason}) when is_atom(reason) or is_binary(reason) do
    conn
    |> put_status(:bad_request)
    |> put_view(FlixBackendWeb.AuthView)
    |> render("error.json", %{error: reason})
  end
end
