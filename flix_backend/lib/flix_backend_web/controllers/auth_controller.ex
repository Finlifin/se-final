defmodule FlixBackendWeb.AuthController do
  use FlixBackendWeb, :controller

  alias FlixBackend.ProfileService
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
        {:ok, user} = ProfileService.get_user_abstract(account.user_id)
        conn
        |> put_status(:ok)
        |> json(ApiResponse.login_success_response(token, account, user))

      {:error, reason} ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response(reason))
    end
  end

  @doc """
  使用手机号和密码登录

  注意：客户端发送的密码已经使用 SHA256 加密过，服务器端会再次加密后存储
  """
  def login_with_password(conn, %{"phone_number" => phone_number, "password" => password}) do
    case Guardian.authenticate(phone_number, password) do
      {:ok, token, account} ->
        {:ok, user} = ProfileService.get_user_abstract(account.user_id)
        conn
        |> put_status(:ok)
        |> json(ApiResponse.login_success_response(token, account, user))

      {:error, :not_found} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response("账号不存在"))
      {:error, :unauthorized} ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response("密码错误"))

      {:error, :password_not_set} ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response("密码错误"))
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

  @doc """
  设置初始密码

  为通过手机号验证码登录但还没有设置密码的用户提供设置初始密码的功能。
  可以通过两种方式验证身份：
  1. 已登录状态（通过 token）
  2. 使用手机号+验证码

  注意：客户端发送的密码已经使用 SHA256 加密过，服务器端会再次加密后存储
  """
  def set_initial_password(conn, %{"new_password" => new_password} = params) do
    # 处理方式1：已登录用户
    if Guardian.Plug.authenticated?(conn) do
      account = Guardian.Plug.current_resource(conn)

      # 检查账户是否已有密码
      case account.hashed_password do
        nil ->
          # 账户没有密码，可以设置初始密码
          case Account.update_password(account, new_password) do
            {:ok, _updated_account} ->
              conn
              |> put_status(:ok)
              |> json(ApiResponse.success_response("密码设置成功"))

            {:error, reason} ->
              conn
              |> put_status(:bad_request)
              |> json(ApiResponse.error_response("密码设置失败: #{inspect(reason)}"))
          end

        _existing_password ->
          # 已有密码，应使用修改密码接口
          conn
          |> put_status(:bad_request)
          |> json(ApiResponse.error_response("账户已有密码，请使用修改密码接口"))
      end
    else
      # 处理方式2：通过手机号+验证码验证身份
      with {:ok, phone_number} <- Map.fetch(params, "phone_number"),
           {:ok, sms_code} <- Map.fetch(params, "sms_code"),
           {:ok, _} <- VerifyCode.verify(phone_number, sms_code),
           account when not is_nil(account) <- Account.get_account_by_phone_number(phone_number) do

        # 检查账户是否已有密码
        case account.hashed_password do
          nil ->
            # 账户没有密码，可以设置初始密码
            case Account.update_password(account, new_password) do
              {:ok, _updated_account} ->
                conn
                |> put_status(:ok)
                |> json(ApiResponse.success_response("密码设置成功"))

              {:error, reason} ->
                conn
                |> put_status(:bad_request)
                |> json(ApiResponse.error_response("密码设置失败: #{inspect(reason)}"))
            end

          _existing_password ->
            # 已有密码，应使用重置密码接口
            conn
            |> put_status(:bad_request)
            |> json(ApiResponse.error_response("账户已有密码，请使用重置密码接口"))
        end
      else
        :error ->
          conn
          |> put_status(:bad_request)
          |> json(ApiResponse.error_response("请提供手机号和验证码"))

        {:error, reason} ->
          conn
          |> put_status(:unauthorized)
          |> json(ApiResponse.unauthorized_response("验证码错误或已过期: #{inspect(reason)}"))

        nil ->
          conn
          |> put_status(:not_found)
          |> json(ApiResponse.not_found_response("账号不存在"))
      end
    end
  end

  @doc """
  修改密码

  注意：客户端发送的密码已经使用 SHA256 加密过，服务器端会再次加密后存储
  """
  def update_password(conn, %{"old_password" => old_password, "new_password" => new_password}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response("用户未登录"))

      account ->
        # First verify the old password
        case Account.verify_password(account, old_password) do
          true ->
            # Old password is correct, proceed with update
            case Account.update_password(account, new_password) do
              {:ok, _account} ->
                # 创建系统通知
                notify_password_changed(account.user_id)

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

  # 发送密码修改成功的系统通知
  defp notify_password_changed(user_id) do
    content = [
      %{type: "text", payload: "您的账户密码已成功修改。如非本人操作，请立即联系客服。"}
    ]

    # 发送系统通知
    FlixBackend.Messaging.send_system_notification(user_id, content)
  end

  @doc """
  重置密码（忘记密码时使用）

  注意：客户端发送的密码已经使用 SHA256 加密过，服务器端会再次加密后存储
  """
  def reset_password(conn, %{"phone_number" => phone_number, "sms_code" => sms_code, "new_password" => new_password}) do
    # 1. 验证短信验证码
    case VerifyCode.verify(phone_number, sms_code) do
      {:ok, _} ->
        # 2. 获取账户
        case Account.get_account_by_phone_number(phone_number) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("账号不存在"))

          account ->
            # 3. 更新密码
            case Account.update_password(account, new_password) do
              {:ok, _updated_account} ->
                conn
                |> put_status(:ok)
                |> json(ApiResponse.success_response("密码重置成功"))

              {:error, reason} ->
                conn
                |> put_status(:bad_request)
                |> json(ApiResponse.error_response("密码重置失败: #{inspect(reason)}"))
            end
        end

      {:error, reason} ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response("验证码错误或已过期: #{inspect(reason)}"))
    end
  end
end
