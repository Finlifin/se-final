defmodule FlixBackend.Repo.Migrations.AddOrderTypeToOrders do
  use Ecto.Migration

  def change do
    alter table(:orders) do
      add :order_type, :string, default: "product"
    end

    # 添加索引以优化按订单类型查询
    create index(:orders, [:order_type])
  end
end
