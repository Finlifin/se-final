defmodule FlixBackend.Repo.Migrations.CreateOrders do
  use Ecto.Migration

  def change do
    create table(:orders, primary_key: false) do
      add :order_id, :string, primary_key: true
      add :buyer_id, references(:users, column: :uid, type: :string), null: false
      add :seller_id, references(:users, column: :uid, type: :string), null: false
      add :product_id, references(:products, column: :id, type: :string), null: false
      # 多余了
      add :order_time, :bigint
      add :price, :float
      add :status, :string, default: "pending"

      timestamps()
    end

    create unique_index(:orders, [:order_id])
    # 为外键创建索引
    create index(:orders, [:buyer_id])
    # 为外键创建索引
    create index(:orders, [:seller_id])
    # 为外键创建索引
    create index(:orders, [:product_id])
  end
end
