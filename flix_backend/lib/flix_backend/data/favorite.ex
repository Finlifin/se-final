defmodule FlixBackend.Data.Favorite do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  schema "favorites" do
    belongs_to :user, FlixBackend.Data.User, type: :binary_id, foreign_key: :user_id, references: :uid
    belongs_to :product, FlixBackend.Data.Product, type: :binary_id, foreign_key: :product_id

    timestamps()
  end

  def changeset(favorite, attrs) do
    favorite
    |> cast(attrs, [:user_id, :product_id])
    |> validate_required([:user_id, :product_id])
    |> unique_constraint([:user_id, :product_id], name: :favorites_user_id_product_id_index)
  end
end
