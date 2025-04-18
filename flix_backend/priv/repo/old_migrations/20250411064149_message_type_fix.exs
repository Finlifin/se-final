defmodule FlixBackend.Repo.Migrations.MessageTypeFix do
  use Ecto.Migration

  def change do
    # drop the old messages table
    drop_if_exists table(:messages)

    create table(:messages, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :client_message_id, :string, null: false
      add :conversation_id, :string, null: false
      add :sender_id, :binary_id, null: false
      add :content_type, :string, null: false  # text, image, video, file
      add :reference_id, :binary_id
      add :content, :text
      add :message_type, FlixBackend.Data.MessageType.type(), null: false
      add :status, FlixBackend.Data.MessageStatus.type(), null: false
      add :server_timestamp, :utc_datetime_usec
      add :client_timestamp, :utc_datetime_usec

      timestamps()
    end
  end
end
