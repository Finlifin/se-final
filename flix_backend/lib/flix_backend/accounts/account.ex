# lib/flix_backend/accounts/account.ex
defmodule FlixBackend.Accounts.Account do
  use Ecto.Schema
  import Ecto.Changeset
  alias FlixBackend.Data.User
  alias __MODULE__
  alias FlixBackend.Repo

  # 使用 UUID 作为主键
  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder,
           only: [
             :id,
             :phone_number,
             :hashed_password,
             :role,
             :user_id
           ]}
  schema "accounts" do
    field :phone_number, :string
    field :hashed_password, :string
    # "user" 或 "admin"
    field :role, :string, default: "user"

    timestamps()

    belongs_to :user, User, foreign_key: :user_id, references: :uid, type: :binary_id
  end

  def changeset(account, attrs) do
    account
    |> cast(attrs, [:phone_number, :password, :role, :user_id])
    # 手机号是必须的
    |> validate_required([:phone_number])
    |> validate_format(:phone_number, ~r/^[1]\d{10}$/, message: "手机号格式不正确")
    |> unique_constraint(:phone_number)
    # 角色必须是 "user" 或 "admin"
    |> validate_inclusion(:role, ["user", "admin"])
    # 密码加密
    |> put_pass_hash()
    # 关联 User
    |> assoc_constraint(:user)
  end

  @doc """
  构建一个用于注册的 changeset，无需密码。
  """
  def registration_changeset(account, attrs) do
    account
    |> cast(attrs, [:phone_number, :user_id])
    |> validate_required([:phone_number, :user_id])
    |> validate_format(:phone_number, ~r/^[1]\d{10}$/, message: "手机号格式不正确")
    |> unique_constraint(:phone_number)
  end

  def get_account_by_phone_number(phone_number) do
    FlixBackend.Repo.get_by(__MODULE__, phone_number: phone_number)
  end

  def get_account!(id) do
    FlixBackend.Repo.get!(__MODULE__, id)
  end

  def get_account_by_user_id(user_id) do
    FlixBackend.Repo.get_by(__MODULE__, user_id: user_id)
  end

  def get_or_create_account_by_phone_number(phone_number) do
    case get_account_by_phone_number(phone_number) do
      nil ->
        Repo.transaction(fn ->
          user = %User{
            # uid: uid,
            phone_number: phone_number,
            user_name: "用户#{phone_number}"
          }
          |> Repo.insert!()

          account =
            %Account{}
            |> registration_changeset(%{
              phone_number: phone_number,
              user_id: user.uid,
              role: "user"
            })
            |> Repo.insert!()

          account
        end)
        |> case do
          {:ok, account} ->
            account

          {:error, _} ->
            # 事务失败，尝试重新获取
            get_account_by_phone_number(phone_number)
        end

      account ->
        # 已存在，直接返回
        account
    end
  end

  def update_password(account, new_password) do
    account
    |> change(%{password: new_password})
    |> put_pass_hash()
    |> Repo.update()
  end

  def verify_password(account, password) do
    case account.hashed_password do
      nil -> false
      hashed_password -> Bcrypt.verify_pass(password, hashed_password)
    end
  end

  defp put_pass_hash(%Ecto.Changeset{valid?: true, changes: %{password: password}} = changeset) do
    change(changeset, hashed_password: Bcrypt.hash_pwd_salt(password))
  end

  defp put_pass_hash(changeset), do: changeset
end
