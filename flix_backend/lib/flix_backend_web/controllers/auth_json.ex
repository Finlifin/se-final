defmodule FlixBackendWeb.AuthJSON do
  alias FlixBackend.Data.User
  @doc """
  渲染登录成功响应
  """
  def login(%{token: token, account: account}) do
    %{
      success: true,
      data: %{
        token: token,
        account: %{
          id: account.id,
          phone_number: account.phone_number,
        },
        user: User.get_user_by_uid(account.user_id),
      }
    }
  end

  @doc """
  渲染短信发送成功响应
  """
  def sms_sent(%{message: message}) do
    %{
      success: true,
      message: message
    }
  end

  @doc """
  token verified
  """
  def token_verified(%{claims: claims, user: user}) do
    %{
      success: true,
      claims: claims,
      user: user,
    }
  end

  @doc """
  渲染错误响应
  """
  def error(%{error: error}) do
    %{
      success: false,
      error: error
    }
  end
end
