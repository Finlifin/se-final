defmodule FlixBackend.Repo.Migrations.AlterProductDescriptionType do
  use Ecto.Migration

  def change do
    alter table(:products) do
      modify :description, :text
    end
  end
end
