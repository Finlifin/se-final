defmodule FlixBackend.Repo.Migrations.CreateMessages do
  use Ecto.Migration

  def change do
    # 创建消息类型枚举
    create_query = "CREATE TYPE message_type AS ENUM ('system_notification', 'system_announcement', 'interaction', 'private_message')"
    execute(create_query, "DROP TYPE message_type")

    # 创建消息内容类型枚举
    create_query = "CREATE TYPE message_content_type AS ENUM ('text', 'image', 'product', 'order', 'comment', 'like', 'favorite', 'system')"
    execute(create_query, "DROP TYPE message_content_type")

    # 创建消息状态枚举
    create_query = "CREATE TYPE message_status AS ENUM ('unread', 'read', 'deleted')"
    execute(create_query, "DROP TYPE message_status")

    # 创建消息表
    create table(:messages, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :sender_id, :binary_id
      add :receiver_id, :binary_id, null: false
      add :content, :map, null: false
      add :content_type, :message_content_type, null: false
      add :message_type, :message_type, null: false
      add :status, :message_status, default: "unread"
      add :reference_id, :binary_id

      timestamps()
    end

    # 添加索引
    create index(:messages, [:receiver_id])
    create index(:messages, [:sender_id])
    create index(:messages, [:status])
    create index(:messages, [:inserted_at])
    create index(:messages, [:message_type])
  end
end
