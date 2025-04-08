defmodule FlixBackend.Data.Campus do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query, only: [from: 2]

  alias FlixBackend.Data.School
  alias FlixBackend.Data.User

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder, only: [:id, :name, :address, :school_id]}
  schema "campuses" do
    field :name, :string
    field :address, :string

    # 关联
    belongs_to :school, School, type: :binary_id
    has_many :users, User

    timestamps()
  end

  def changeset(campus, attrs) do
    campus
    |> cast(attrs, [:name, :address, :school_id])
    |> validate_required([:name, :address, :school_id])
    |> foreign_key_constraint(:school_id)
  end

  def get_campuses_by_school(school_id) do
    query = from c in __MODULE__,
            where: c.school_id == ^school_id

    FlixBackend.Repo.all(query)
  end
end
