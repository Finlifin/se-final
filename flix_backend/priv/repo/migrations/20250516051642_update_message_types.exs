defmodule FlixBackend.Repo.Migrations.UpdateMessageTypes do
  use Ecto.Migration

  def change do
    # 创建消息类型的枚举类型
    execute """
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'message_type') THEN
        CREATE TYPE message_type AS ENUM (
          'system',
          'notification',
          'chat',
          'order',
          'payment'
        );
      ELSE
        -- 如果类型已存在，更新以确保所有值都包含在内
        ALTER TYPE message_type ADD VALUE IF NOT EXISTS 'system';
        ALTER TYPE message_type ADD VALUE IF NOT EXISTS 'notification';
        ALTER TYPE message_type ADD VALUE IF NOT EXISTS 'chat';
        ALTER TYPE message_type ADD VALUE IF NOT EXISTS 'order';
        ALTER TYPE message_type ADD VALUE IF NOT EXISTS 'payment';
      END IF;
    END
    $$;
    """, "DROP TYPE IF EXISTS message_type;"

    # 如果 messages 表已存在，更新其 message_type 列
    execute """
    DO $$
    BEGIN
      IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'messages') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'messages' AND column_name = 'message_type') THEN
          ALTER TABLE messages ALTER COLUMN message_type TYPE message_type USING message_type::text::message_type;
        ELSE
          ALTER TABLE messages ADD COLUMN message_type message_type;
        END IF;
      END IF;
    END
    $$;
    """
  end
end
