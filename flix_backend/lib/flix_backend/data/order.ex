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
             :payment_time
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
      :payment_time
    ])
    |> validate_required([:buyer_id, :seller_id, :product_id, :order_time, :price])
    |> unique_constraint(:order_id)
  end
end
