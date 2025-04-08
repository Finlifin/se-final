defmodule FlixBackend.Repo.Migrations.CreateFavorites do
  use Ecto.Migration

  def change do
    create table(:favorites, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :user_id, references(:users, type: :binary_id, column: :uid, on_delete: :delete_all)
      add :product_id, references(:products, type: :binary_id, on_delete: :delete_all)

      timestamps()
    end

    create index(:favorites, [:user_id])
    create index(:favorites, [:product_id])
    create unique_index(:favorites, [:user_id, :product_id], name: :favorites_user_id_product_id_index)
  end
end
