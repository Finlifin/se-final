defmodule FlixBackend.Data.Product do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder,
           only: [
             :id,
             :seller_id,
             :title,
             :description,
             :price,
             :images,
             :category,
             :condition,
             :location,
             :post_time,
             :status,
             :view_count,
             :favorite_count,
             :tags,
             :available_delivery_methods,
             :campus_id
           ]}
  schema "products" do
    # field :seller_id, :string  # 引用 User 的 UID
    field :title, :string
    field :description, :string
    field :price, :float
    field :images, {:array, :string}
    field :category, :string
    field :condition, :string
    field :location, :string
    field :post_time, :integer
    field :status, FlixBackend.Data.ProductStatus, default: :available
    field :view_count, :integer, default: 0
    field :favorite_count, :integer, default: 0

    field :tags, {:array, :string}, default: []
    field :available_delivery_methods, {:array, :string}, default: []

    # 不需要在模型中定义 search_vector 字段，由PostgreSQL在后台管理

    timestamps()

    belongs_to :user, FlixBackend.Data.User,
      foreign_key: :seller_id,
      references: :uid,
      type: :binary_id

    # 添加校区关联
    belongs_to :campus, FlixBackend.Data.Campus,
      foreign_key: :campus_id,
      references: :id,
      type: :binary_id
  end

  def changeset(product, attrs) do
    product
    |> cast(attrs, [
      :seller_id,
      :title,
      :description,
      :price,
      :images,
      :category,
      :condition,
      :location,
      :post_time,
      :status,
      :view_count,
      :favorite_count,
      :tags,
      :available_delivery_methods,
      :campus_id
    ])
    |> validate_required([
      :seller_id,
      :title,
      :description,
      :price,
      :images,
      :category,
      :condition,
      :location
    ])
  end
end
