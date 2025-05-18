defmodule FlixBackend.Data.School do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder, only: [:id, :name, :code]}
  schema "schools" do
    field :name, :string
    field :code, :string

    # 反向引用校区
    has_many :campuses, FlixBackend.Data.Campus, foreign_key: :school_id

    timestamps()
  end

  def changeset(school, attrs) do
    school
    |> cast(attrs, [:name, :code])
    |> validate_required([:name])
    |> unique_constraint(:name)
  end
end
