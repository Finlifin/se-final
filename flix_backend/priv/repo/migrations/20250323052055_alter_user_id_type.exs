defmodule FlixBackend.Repo.Migrations.AlterUserIdType do
  use Ecto.Migration

  def change do
    # 创建 UUID 扩展（如果尚未创建）
    execute "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\""

    # 直接删除所有表并重建
    drop_if_exists table(:orders)
    drop_if_exists table(:products)
    drop_if_exists table(:accounts)
    drop_if_exists table(:users)

    # 重新创建 users 表，现在使用 UUID 作为主键
    create table(:users, primary_key: false) do
      add :uid, :uuid, primary_key: true, default: fragment("uuid_generate_v4()")
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
    create unique_index(:users, [:phone_number])

    # 重新创建 accounts 表
    create table(:accounts, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :phone_number, :string
      add :hashed_password, :string
      add :role, :string, default: "user"
      # 现在 user_id 是 UUID 类型
      add :user_id, references(:users, column: :uid, type: :uuid)

      timestamps()
    end

    create unique_index(:accounts, [:phone_number])
    create index(:accounts, [:user_id])

    # 添加 account_id 到 users 表
    alter table(:users) do
      add :account_id, references(:accounts, column: :id, type: :binary_id), null: true
    end

    # 重新创建 products 表
    create table(:products, primary_key: false) do
      add :id, :string, primary_key: true
      # 外键，现在关联到 UUID 类型的 uid
      add :seller_id, references(:users, column: :uid, type: :uuid), null: false
      add :title, :string
      add :description, :string
      add :price, :float
      add :images, {:array, :string}
      add :category, :string
      add :condition, :string
      add :location, :string
      add :post_time, :bigint
      add :status, :string, default: "available"
      add :view_count, :integer, default: 0
      add :favorite_count, :integer, default: 0

      timestamps()
    end

    create unique_index(:products, [:id])
    create index(:products, [:seller_id])
    create index(:products, [:title])
    create index(:products, [:category, :condition])

    # 重新创建 orders 表
    create table(:orders, primary_key: false) do
      add :order_id, :string, primary_key: true
      add :buyer_id, references(:users, column: :uid, type: :uuid), null: false
      add :seller_id, references(:users, column: :uid, type: :uuid), null: false
      add :product_id, references(:products, column: :id, type: :string), null: false
      add :order_time, :bigint
      add :price, :float
      add :status, :string, default: "pending"

      timestamps()
    end

    create unique_index(:orders, [:order_id])
    create index(:orders, [:buyer_id])
    create index(:orders, [:seller_id])
    create index(:orders, [:product_id])
  end
end
