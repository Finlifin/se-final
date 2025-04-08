defmodule FlixBackend.Repo.Migrations.AddSchoolEntity do
  use Ecto.Migration

  def change do
    # 创建学校表
    create table(:schools, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :name, :string, null: false
      add :code, :string, null: false

      timestamps()
    end

    # 添加学校代码唯一性约束
    create unique_index(:schools, [:code])

    # 创建校区表
    create table(:campuses, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :name, :string, null: false
      add :address, :string, null: false
      add :school_id, references(:schools, type: :binary_id, on_delete: :delete_all), null: false

      timestamps()
    end

    # 添加索引加速查询
    create index(:campuses, [:school_id])

    # 为用户表添加学校和校区字段
    alter table(:users) do
      remove :address, :string
      add :school_id, references(:schools, type: :binary_id, on_delete: :nilify_all)
      add :campus_id, references(:campuses, type: :binary_id, on_delete: :nilify_all)
    end

    # 添加索引加速查询
    create index(:users, [:school_id])
    create index(:users, [:campus_id])
  end
end
