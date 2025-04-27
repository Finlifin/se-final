defmodule FlixBackend.Repo.Migrations.MigrateMessageConversationIdTypeToString do
  use Ecto.Migration

  def change do
    alter table(:messages) do
      modify :conversation_id, :string, null: false
    end
  end
end
