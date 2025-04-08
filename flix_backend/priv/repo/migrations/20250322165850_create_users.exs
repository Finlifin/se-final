defmodule FlixBackend.Repo.Migrations.CreateUsers do
  use Ecto.Migration

  def change do
    # 关闭自增主键
    create table(:users, primary_key: false) do
      add :uid, :string, primary_key: true
      add :phone_number, :string
      add :user_name, :string
      add :avatar_url, :string
      add :address, :string
      add :balance, :integer, default: 0
      add :published_product_ids, {:array, :string}, default: []
      add :sold_product_ids, {:array, :string}, default: []
      add :purchased_product_ids, {:array, :string}, default: []
      add :favorited_product_ids, {:array, :string}, default: []

      timestamps()
    end

    create unique_index(:users, [:uid])
    # 如果需要
    create unique_index(:users, [:phone_number])
  end
end
