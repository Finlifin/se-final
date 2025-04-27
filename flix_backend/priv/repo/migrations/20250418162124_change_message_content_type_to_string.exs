defmodule FlixBackend.Repo.Migrations.ChangeMessageContentTypeToString do
  use Ecto.Migration

  def change do
    alter table(:messages) do
      modify :content, :string, default: "[]"
    end

    # 迁移现有数据
    execute("""
    UPDATE messages
    SET content = '[]'
    WHERE content IS NULL OR content = ''
    """)
  end
end
