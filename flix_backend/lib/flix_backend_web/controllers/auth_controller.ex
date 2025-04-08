defmodule FlixBackendWeb.AuthController do
  use FlixBackendWeb, :controller

  alias FlixBackend.Data.User
  alias FlixBackend.Guardian
  alias FlixBackend.Accounts.VerifyCode
  alias FlixBackend.Accounts.Account
  alias FlixBackendWeb.ApiResponse # Import ApiResponse

  action_fallback FlixBackendWeb.FallbackController

  @doc """
  使用手机号和验证码登录
  """
  def login_with_sms(conn, %{"phone_number" => phone_number, "sms_code" => sms_code}) do
    case Guardian.authenticate(phone_number, :sms_code, sms_code) do
      {:ok, token, account} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.login_success_response(token, account)) # Use ApiResponse

      {:error, reason} ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response(reason)) # Use ApiResponse
    end
  end

  @doc """
  使用手机号和密码登录
  """
  def login_with_password(conn, %{"phone_number" => phone_number, "password" => password}) do
    case Guardian.authenticate(phone_number, password) do
      {:ok, token, account} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.login_success_response(token, account)) # Use ApiResponse

      {:error, :not_found} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response("账号不存在")) # Use ApiResponse

      {:error, :unauthorized} ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response("密码错误")) # Use ApiResponse
    end
  end

  @doc """
  发送短信验证码
  """
  def send_sms_code(conn, %{"phone_number" => phone_number}) do
    # 验证手机号格式
    if Regex.match?(~r/^[1]\d{10}$/, phone_number) do
      case VerifyCode.generate_and_send(phone_number) do
        {:ok, _} ->
          conn
          |> put_status(:ok)
          |> json(ApiResponse.sms_sent_response()) # Use ApiResponse

        {:error, reason} -> # Handle potential errors from VerifyCode
          conn
          |> put_status(:bad_request)
          |> json(ApiResponse.error_response("发送验证码失败: #{inspect(reason)}")) # Use ApiResponse
      end
    else
      conn
      |> put_status(:bad_request)
      |> json(ApiResponse.error_response("手机号格式不正确")) # Use ApiResponse
    end
  end

  @doc """
  验证token的有效性
  """
  def verify_token(conn, %{"token" => token}) do
    case Guardian.decode_and_verify(token) do
      {:ok, claims} ->
        # Consider handling potential errors from get_account! and get_user_by_uid
        account = Account.get_account!(claims["sub"])
        user = User.get_user_by_uid(account.user_id)
        user_abstract = User.get_user_abstract_by_uid(user.uid)

        conn
        |> put_status(:ok)
        |> json(ApiResponse.token_verified_response(claims, user_abstract)) # Use ApiResponse

      {:error, reason} ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response(reason)) # Use ApiResponse
    end
  end

  def update_password(conn, %{"old_password" => old_password, "new_password" => new_password}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())

      account ->
        # First verify the old password
        case Account.verify_password(account, old_password) do
          true ->
            # Old password is correct, proceed with update
            case Account.update_password(account, new_password) do
              {:ok, _account} ->
                conn
                |> put_status(:ok)
                |> json(ApiResponse.success_response("密码更新成功"))

              {:error, reason} ->
                conn
                |> put_status(:bad_request)
                |> json(ApiResponse.error_response(reason))
            end

          false ->
            # Old password is incorrect
            conn
            |> put_status(:unauthorized)
            |> json(ApiResponse.unauthorized_response("原密码不正确"))
        end
    end
  end
end
