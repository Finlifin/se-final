defmodule FlixBackend.Repo.Migrations.ChangeUserBalanceToFloat do
  use Ecto.Migration

  def change do
    alter table(:users) do
      modify :balance, :float, default: 0.0
    end
  end
end
