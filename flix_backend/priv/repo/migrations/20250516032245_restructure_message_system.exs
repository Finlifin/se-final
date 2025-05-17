defmodule FlixBackend.Repo.Migrations.RestructureMessageSystem do
  use Ecto.Migration

  def change do
    # 删除旧的相关表（如果存在）
    drop_if_exists table(:user_conversations)
    drop_if_exists table(:conversations)
    drop_if_exists table(:events)
    drop_if_exists table(:messages)

    # 创建新的消息表
    create table(:messages, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :message_id, :string, null: false
      add :sender, :binary_id
      add :receiver, :binary_id, null: false
      add :content, {:array, :map}, null: false, default: []
      add :message_type, :message_type, null: false
      add :read, :boolean, default: false, null: false

      timestamps()
    end

    # 创建索引以提高查询性能
    create index(:messages, [:receiver])
    create index(:messages, [:sender])
    create index(:messages, [:message_id], unique: true)
    create index(:messages, [:message_type])
    create index(:messages, [:receiver, :read])
    create index(:messages, [:inserted_at])
  end
end
