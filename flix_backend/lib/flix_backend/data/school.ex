defmodule FlixBackend.Data.School do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query, only: [from: 2]

  alias FlixBackend.Data.Campus
  alias FlixBackend.Data.User

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder, only: [:id, :name, :code, :campuses]}
  schema "schools" do
    field :name, :string
    field :code, :string

    # 关联
    has_many :campuses, Campus
    has_many :users, User

    timestamps()
  end

  def changeset(school, attrs) do
    school
    |> cast(attrs, [:name, :code])
    |> validate_required([:name, :code])
    |> unique_constraint(:code)
  end

  def get_all_schools do
    FlixBackend.Repo.all(__MODULE__)
  end

  def get_school_with_campuses(id) do
    FlixBackend.Repo.get(__MODULE__, id)
    |> FlixBackend.Repo.preload(:campuses)
  end
end
