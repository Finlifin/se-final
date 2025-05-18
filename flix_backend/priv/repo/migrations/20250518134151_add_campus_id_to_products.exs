defmodule FlixBackend.Repo.Migrations.AddCampusIdToProducts do
  use Ecto.Migration

  def change do
    alter table(:products) do
      # 添加校区ID字段，可为空，外键引用campuses表的id字段
      add :campus_id, references(:campuses, type: :binary_id), null: true
    end

    # 添加索引以提高查询性能
    create index(:products, [:campus_id])
  end
end
