defmodule FlixBackendWeb.ErrorView do
  # 直接定义视图，避免使用宏

  def render("404.json", _assigns) do
    %{success: false, error: "未找到资源"}
  end

  def render("401.json", _assigns) do
    %{success: false, error: "未认证"}
  end

  def render("500.json", _assigns) do
    %{success: false, error: "服务器内部错误"}
  end

  # 回退视图，当没有匹配的视图时
  def render(template, _assigns) do
    %{success: false, error: Phoenix.Controller.status_message_from_template(template)}
  end
end
