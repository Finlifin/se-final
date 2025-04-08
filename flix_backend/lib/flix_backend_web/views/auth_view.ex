defmodule FlixBackendWeb.AuthView do
  # 不使用 Phoenix 的视图宏，直接定义我们自己的视图
  # 这样可以避免依赖于 FlixBackendWeb.view/0 函数

  # 渲染登录成功的响应
  def render("login.json", %{token: token, account: account}) do
    %{
      success: true,
      data: %{
        token: token,
        account: %{
          id: account.id,
          phone_number: account.phone_number,
          role: account.role,
          user_id: account.user_id
        }
      }
    }
  end

  # 渲染短信发送成功的响应
  def render("sms_sent.json", %{message: message}) do
    %{
      success: true,
      message: message
    }
  end

  # 渲染错误响应
  def render("error.json", %{error: error}) do
    %{
      success: false,
      error: error
    }
  end
end
