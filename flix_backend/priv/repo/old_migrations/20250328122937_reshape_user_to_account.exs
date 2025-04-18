defmodule FlixBackend.Repo.Migrations.ReshapeUserToAccount do
  use Ecto.Migration

  def change do
    # 删除 users 表中的外键约束（如果存在）
    drop_if_exists constraint("users", "users_account_id_fkey")

    # 修改 users 表，移除 account_id 字段
    alter table(:users) do
      remove :account_id
    end
  end
end
