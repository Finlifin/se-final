defmodule FlixBackend.Repo.Migrations.InstantMessaging do
  use Ecto.Migration

  def change do
    # 消息表 - 新建消息表
    create table(:messages, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :client_message_id, :string, null: false
      add :conversation_id, :string, null: false
      add :sender_id, :binary_id, null: false
      add :content_type, :string, null: false  # text, image, video, file
      add :reference_id, :binary_id
      add :content, :text
      add :status, FlixBackend.Data.MessageStatus.type(), null: false
      add :server_timestamp, :utc_datetime_usec
      add :client_timestamp, :utc_datetime_usec

      timestamps()
    end

    # 会话表 - 用于管理用户之间的聊天会话
    create table(:conversations, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :conversation_id, :string, null: false
      add :type, :string, null: false  # private, group
      add :participant_ids, {:array, :binary_id}, null: false
      add :last_message_id, :binary_id
      add :last_message_content, :string
      add :last_message_timestamp, :utc_datetime_usec

      timestamps()
    end

    # 用户会话关系表 - 记录用户在会话中的状态
    create table(:user_conversations, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :user_id, :binary_id, null: false
      add :conversation_id, :string, null: false
      add :last_read_message_id, :binary_id
      add :unread_count, :integer, default: 0, null: false
      add :is_pinned, :boolean, default: false
      add :is_muted, :boolean, default: false
      add :draft, :text

      timestamps()
    end

    # 事件表 - 用于同步机制
    create table(:events, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :event_type, :string, null: false
      add :payload, :map, null: false
      add :event_timestamp, :utc_datetime_usec, null: false
      add :target_user_id, :binary_id, null: false

      timestamps()
    end

    # 创建索引
    create index(:messages, [:client_message_id])
    create index(:messages, [:conversation_id])
    create index(:messages, [:server_timestamp])

    create unique_index(:conversations, [:conversation_id])
    create index(:conversations, [:updated_at])

    create unique_index(:user_conversations, [:user_id, :conversation_id])
    create index(:user_conversations, [:user_id])

    create index(:events, [:target_user_id, :event_timestamp])
  end
end
