defmodule FlixBackend.Repo.Migrations.CreateComments do
  use Ecto.Migration

  def change do
    # 先创建评论状态枚举类型
    execute "CREATE TYPE comment_status AS ENUM ('active', 'deleted', 'hidden')"

    # 创建评论表
    create table(:comments, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :product_id, references(:products, type: :binary_id, on_delete: :delete_all), null: false
      add :user_id, references(:users, column: :uid, type: :binary_id, on_delete: :nilify_all), null: false
      add :parent_id, references(:comments, type: :binary_id, on_delete: :nilify_all)
      add :root_id, references(:comments, type: :binary_id, on_delete: :nilify_all)
      add :content, :text, null: false
      add :likes_count, :integer, default: 0
      add :replies_count, :integer, default: 0
      add :status, :comment_status, default: "active"

      timestamps()
    end

    # 索引
    create index(:comments, [:product_id])
    create index(:comments, [:user_id])
    create index(:comments, [:parent_id])
    create index(:comments, [:root_id])

    # 点赞表
    create table(:comment_likes, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :comment_id, references(:comments, type: :binary_id, on_delete: :delete_all), null: false
      add :user_id, references(:users, column: :uid, type: :binary_id, on_delete: :nilify_all), null: false

      timestamps()
    end

    # 创建唯一索引确保用户只能点赞一次
    create unique_index(:comment_likes, [:comment_id, :user_id])
    create index(:comment_likes, [:user_id])
  end
end
