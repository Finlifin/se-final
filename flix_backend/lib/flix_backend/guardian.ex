# lib/flix_backend/guardian.ex
defmodule FlixBackend.Guardian do
  use Guardian, otp_app: :flix_backend

  alias FlixBackend.Accounts.Account
  alias FlixBackend.Accounts.VerifyCode

  # 根据 subject (通常是用户 ID) 查找用户
  def subject_for_token(account, _claims) when is_map(account) do
    {:ok, to_string(account.id)}
  end

  def subject_for_token(account_id, _claims) when is_binary(account_id) do
    {:ok, account_id}
  end

  def subject_for_token(_, _) do
    {:error, :reason_unsupported}
  end

  # 根据 subject 查找 Account
  def resource_from_claims(claims) do
    account_id = Map.get(claims, "sub")

    if is_nil(account_id) do
      {:error, :invalid_claims}
    else
      try do
        {:ok, Account.get_account!(account_id)}
      rescue
        Ecto.NoResultsError -> {:error, :account_not_found}
      end
    end
  end

  def authenticate(phone_number, password) do
    case Account.get_account_by_phone_number(phone_number) do
      nil ->
        {:error, :not_found}

      account ->
        case Bcrypt.verify_pass(password, account.hashed_password) do
          true ->
            create_token(account)

          false ->
            {:error, :unauthorized}
        end
    end
  end

  def authenticate(phone_number, :sms_code, sms_code) do
    # 1. 检查验证码是否正确
    case VerifyCode.verify(phone_number, sms_code) do
      {:ok, _code} ->
        # 2. 根据手机号查找或创建 Account
        account = Account.get_or_create_account_by_phone_number(phone_number)
        # 3. 创建 JWT
        create_token(account)

      {:error, reason} ->
        # 返回验证码错误的原因
        {:error, reason}
    end
  end

  defp create_token(account) do
    {:ok, token, _claims} = encode_and_sign(account)
    {:ok, token, account}
  end
end
