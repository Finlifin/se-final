defmodule FlixBackend.Repo.Migrations.FixUserAddresses do
  use Ecto.Migration

  def change do
    alter table(:users) do
      add :addresses, {:array, :string}, default: []
      add :current_address, :string
    end
  end
end
