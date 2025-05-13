defmodule FlixBackend.Data.Order do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:order_id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :order_id}
  @derive {Jason.Encoder,
           only: [
             :order_id,
             :buyer_id,
             :seller_id,
             :product_id,
             :order_time,
             :price,
             :status,
             :delivery_method,
             :delivery_address,
             :delivery_time,
             :delivery_fee,
             :payment_method,
             :payment_time,
             :order_type
           ]}
  schema "orders" do
    field :order_time, :integer
    field :price, :float
    field :delivery_method, :string
    field :delivery_address, :string
    field :delivery_time, :integer
    field :delivery_fee, :float
    field :payment_method, :string
    field :payment_time, :integer
    field :status, FlixBackend.Data.OrderStatus, default: :pending
    field :order_type, :string, default: "product"  # "product" or "recharge"

    timestamps()

    belongs_to :seller, FlixBackend.Data.User,
      foreign_key: :seller_id,
      references: :uid,
      type: :binary_id

    belongs_to :buyer, FlixBackend.Data.User,
      foreign_key: :buyer_id,
      references: :uid,
      type: :binary_id

    belongs_to :product, FlixBackend.Data.Product,
      foreign_key: :product_id,
      references: :id,
      type: :binary_id
  end

  def changeset(order, attrs) do
    order
    |> cast(attrs, [
      :buyer_id,
      :seller_id,
      :product_id,
      :order_time,
      :price,
      :status,
      :delivery_method,
      :delivery_address,
      :delivery_time,
      :delivery_fee,
      :payment_method,
      :payment_time,
      :order_type
    ])
    |> validate_required([:buyer_id, :price])
    |> validate_order_type()
    |> unique_constraint(:order_id)
  end

  # 验证订单类型相关字段
  defp validate_order_type(changeset) do
    case get_field(changeset, :order_type) do
      "product" ->
        changeset
        |> validate_required([:seller_id, :product_id])
      "recharge" ->
        changeset
      _ ->
        add_error(changeset, :order_type, "must be either product or recharge")
    end
  end
end
