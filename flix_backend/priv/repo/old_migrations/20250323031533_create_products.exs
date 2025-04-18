defmodule FlixBackend.Repo.Migrations.CreateProducts do
  use Ecto.Migration

  def change do
    create table(:products, primary_key: false) do
      add :id, :string, primary_key: true
      # 外键，关联到 users 表的 uid 字段
      add :seller_id, references(:users, column: :uid, type: :string), null: false
      add :title, :string
      add :description, :string
      add :price, :float
      add :images, {:array, :string}
      add :category, :string
      add :condition, :string
      add :location, :string
      # 使用 bigint 存储时间戳
      add :post_time, :bigint
      # 使用字符串存储枚举值
      add :status, :string, default: "available"
      add :view_count, :integer, default: 0
      add :favorite_count, :integer, default: 0

      timestamps()
    end

    create unique_index(:products, [:id])
    # 为外键创建索引，提高查询性能
    create index(:products, [:seller_id])
    # 为 title 创建索引
    create index(:products, [:title])
    # 组合索引
    create index(:products, [:category, :condition])
  end
end
