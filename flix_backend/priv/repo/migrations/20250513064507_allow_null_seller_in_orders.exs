defmodule FlixBackend.Repo.Migrations.AllowNullSellerInOrders do
  use Ecto.Migration

  def change do
    # 先删除现有的外键约束
    drop constraint(:orders, "orders_seller_id_fkey")
    drop constraint(:orders, "orders_product_id_fkey")

    # 修改列为可空，并添加新的外键约束
    alter table(:orders) do
      modify :seller_id, references(:users, column: :uid, type: :binary_id, on_delete: :nilify_all), null: true
      modify :product_id, references(:products, type: :binary_id, on_delete: :nilify_all), null: true
    end
  end
end
