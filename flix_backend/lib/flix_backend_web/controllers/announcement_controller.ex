defmodule FlixBackendWeb.AnnouncementController do
  use FlixBackendWeb, :controller

  # 获取系统公告列表
  # GET /api/v1/announcements?limit=10&offset=0
  def index(conn, params) do
    limit = Map.get(params, "limit", "10") |> String.to_integer()
    offset = Map.get(params, "offset", "0") |> String.to_integer()

    # 在具体实现时编写获取系统公告逻辑

    conn
    |> put_status(:ok)
    |> json(%{data: []})
  end

  # 获取单条系统公告详情
  # GET /api/v1/announcements/:id
  def show(conn, %{"id" => id}) do
    # 在具体实现时编写获取单条系统公告逻辑

    conn
    |> put_status(:ok)
    |> json(%{data: %{}})
  end
end
