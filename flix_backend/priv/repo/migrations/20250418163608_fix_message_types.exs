defmodule FlixBackend.Repo.Migrations.FixMessageTypes do
  use Ecto.Migration

  def change do
    alter table(:messages) do
      modify :client_message_id, :binary_id, null: false
      modify :conversation_id, :binary_id, null: false
      modify :content, {:array, :map}, null: false, default: []
    end
  end
end
