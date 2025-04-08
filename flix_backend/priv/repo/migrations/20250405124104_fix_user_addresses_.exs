defmodule FlixBackend.Repo.Migrations.FixUserAddresses do
  use Ecto.Migration

  def change do
    alter table(:users) do
      remove :addresses
      add :addresses, {:array, :string}, default: []
    end
  end
end
