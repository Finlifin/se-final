defmodule FlixBackend.Guardian.AuthPipeline do
  use Guardian.Plug.Pipeline,
    otp_app: :flix_backend,
    module: FlixBackend.Guardian,
    error_handler: FlixBackend.Guardian.AuthErrorHandler

  # 如果请求中有认证令牌，则验证它
  plug Guardian.Plug.VerifyHeader, scheme: "Bearer"
  # 验证令牌是否未过期
  plug Guardian.Plug.EnsureAuthenticated
  # 将经过验证的资源加载到连接中
  plug Guardian.Plug.LoadResource, allow_blank: false
end
