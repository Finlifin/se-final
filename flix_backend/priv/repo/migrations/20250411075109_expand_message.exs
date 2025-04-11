defmodule FlixBackend.Repo.Migrations.ExpandMessage do
  use Ecto.Migration

  def change do
    alter table(:messages) do
      remove :content
      remove :content_type
      add :content, {:array, :map}, null: false, default: []
    end
  end
end
