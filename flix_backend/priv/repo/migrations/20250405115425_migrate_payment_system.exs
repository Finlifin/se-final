defmodule FlixBackend.Repo.Migrations.MigratePaymentSystem do
  use Ecto.Migration

  def change do
    alter table(:orders) do
      add :delivery_method, :string
      add :delivery_address, :string
      add :delivery_time, :integer
      add :delivery_fee, :float
      add :payment_method, :string
      add :payment_time, :integer
    end

    alter table(:products) do
      add :tags, {:array, :string}, default: []
      add :available_delivery_methods, {:array, :string}, default: []
    end
  end
end
