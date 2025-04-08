defmodule FlixBackend.Repo.Migrations.CreateAccounts do
  use Ecto.Migration

  def change do
    create table(:accounts, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :phone_number, :string
      add :hashed_password, :string
      add :role, :string, default: "user"
      # 关联到 users 表的 uid
      add :user_id, references(:users, column: :uid, type: :string)

      timestamps()
    end

    create unique_index(:accounts, [:phone_number])
    create index(:accounts, [:user_id])

    alter table(:users) do
      # 关联到 accounts 表的 id
      add :account_id, references(:accounts, column: :id, type: :binary_id), null: false
    end
  end
end
