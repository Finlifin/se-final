defmodule FlixBackendWeb.UserSocket do
  use Phoenix.Socket

  # 定义通道路由
  channel "user:*", FlixBackendWeb.MessageChannel

  # 处理连接
  @impl true
  def connect(_params, socket, _connect_info) do
    {:ok, socket}
  end

  # Socket id处理
  @impl true
  def id(_socket), do: nil
end
