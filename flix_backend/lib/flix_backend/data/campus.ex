defmodule FlixBackend.Data.Campus do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder, only: [:id, :name, :school_id, :address]}
  schema "campuses" do
    field :name, :string
    field :address, :string

    # 引用学校
    belongs_to :school, FlixBackend.Data.School,
      foreign_key: :school_id,
      type: :binary_id

    timestamps()
  end

  def changeset(campus, attrs) do
    campus
    |> cast(attrs, [:name, :school_id, :address])
    |> validate_required([:name, :school_id, :address])
    |> foreign_key_constraint(:school_id, message: "所选学校不存在")
  end
end
