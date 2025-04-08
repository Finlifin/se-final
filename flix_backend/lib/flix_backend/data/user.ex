defmodule FlixBackend.Data.User do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query, only: [from: 2]

  alias FlixBackend.Accounts.Account
  alias FlixBackend.Data.School
  alias FlixBackend.Data.Campus

  # 使用 UUID 作为主键
  @primary_key {:uid, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :uid}
  @derive {Jason.Encoder,
           only: [
             :uid,
             :phone_number,
             :user_name,
             :avatar_url,
             :addresses,
             :current_address,
             :balance,
             :published_product_ids,
             :sold_product_ids,
             :purchased_product_ids,
             :favorite_product_ids,
             :school_id,
             :campus_id,
           ]}
  schema "users" do
    field :phone_number, :string
    field :user_name, :string
    field :avatar_url, :string
    field :addresses, {:array, :string}, default: []
    field :current_address, :string
    field :balance, :integer, default: 0
    field :published_product_ids, {:array, :binary_id}, default: []
    field :sold_product_ids, {:array, :binary_id}, default: []
    field :purchased_product_ids, {:array, :binary_id}, default: []
    field :favorite_product_ids, {:array, :binary_id}, default: []

    # 添加学校和校区关联
    belongs_to :school, School, foreign_key: :school_id, type: :binary_id
    belongs_to :campus, Campus, foreign_key: :campus_id, type: :binary_id

    timestamps()
  end

  def get_user_by_account_id(account_id) do
    account = FlixBackend.Repo.get_by(Account, id: account_id)

    if account do
      FlixBackend.Repo.get_by(__MODULE__, uid: account.user_id)
    else
      nil
    end
  end

  def get_user_abstract_by_account_id(account_id) do
    account = FlixBackend.Repo.get_by(Account, id: account_id)

    if account do
      query =
        from u in __MODULE__,
          where: u.uid == ^account.user_id,
          select: %{
            uid: u.uid,
            user_name: u.user_name,
            avatar_url: u.avatar_url
          }

      # Execute the query and return the result
      FlixBackend.Repo.one(query)
    else
      nil
    end
  end

  def get_user_abstract_by_uid(uid) do
    query =
      from u in __MODULE__,
        where: u.uid == ^uid,
        select: %{
          uid: u.uid,
          user_name: u.user_name,
          avatar_url: u.avatar_url
        }

    # Execute the query and return the result
    FlixBackend.Repo.one(query)
  end

  def get_user_by_uid(uid) do
    FlixBackend.Repo.get(__MODULE__, uid)
  end

  def add_product_id(user_id, product_id) do
    user = FlixBackend.Repo.get(__MODULE__, user_id)

    # cast product_id from string to binary_id
    product_id = Ecto.UUID.cast!(product_id)

    # 更新用户的已发布商品ID列表
    changeset =
      user
      |> changeset(%{published_product_ids: [product_id | user.published_product_ids]})

    FlixBackend.Repo.update(changeset)
  end

  @spec changeset(
          {map(),
           %{
             optional(atom()) =>
               atom()
               | {:array | :assoc | :embed | :in | :map | :parameterized | :supertype | :try,
                  any()}
           }}
          | %{
              :__struct__ => atom() | %{:__changeset__ => any(), optional(any()) => any()},
              optional(atom()) => any()
            },
          :invalid | %{optional(:__struct__) => none(), optional(atom() | binary()) => any()}
        ) :: Ecto.Changeset.t()
  def changeset(user, attrs) do
    user
    |> cast(attrs, [
      :uid,
      :phone_number,
      :user_name,
      :avatar_url,
      :address,
      :balance,
      :published_product_ids,
      :sold_product_ids,
      :purchased_product_ids,
      :favorited_product_ids,
      :school_id,
      :campus_id
    ])
    # 根据需要验证必填字段
    |> validate_required([:uid, :phone_number, :user_name])
    # 确保uid的唯一性
    |> unique_constraint(:uid)
    # 确保手机号的唯一性（如果需要）
    |> unique_constraint(:phone_number)
    # 确保校区属于选择的学校
    |> validate_campus_belongs_to_school()
  end

  # 验证校区属于选择的学校
  defp validate_campus_belongs_to_school(changeset) do
    school_id = get_field(changeset, :school_id)
    campus_id = get_field(changeset, :campus_id)

    if school_id && campus_id do
      campus = FlixBackend.Repo.get(Campus, campus_id)

      if campus && campus.school_id == school_id do
        changeset
      else
        add_error(changeset, :campus_id, "所选校区不属于选择的学校")
      end
    else
      changeset
    end
  end
end

defmodule FlixBackend.Data.UserAbstract do
  use Ecto.Schema

  # 没有主键
  @primary_key false
  schema "" do
    field :uid, :string
    field :user_name, :string
    field :avatar_url, :string
  end
end

defmodule FlixBackend.Data.ProductAbstract do
  use Ecto.Schema
  @primary_key false
  schema "" do
    field :id, :string
    field :title, :string
    field :price, :float
    field :images, {:array, :string}
    field :status, FlixBackend.Data.ProductStatus
  end
end
