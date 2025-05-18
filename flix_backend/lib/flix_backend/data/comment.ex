defmodule FlixBackend.Data.Comment do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder, only: [
    :id,
    :product_id,
    :user_id,
    :parent_id,
    :root_id,
    :content,
    :likes_count,
    :replies_count,
    :status,
    :inserted_at,
    :updated_at,
    :user,
    :is_liked
  ]}

  schema "comments" do
    field :content, :string
    field :likes_count, :integer, default: 0
    field :replies_count, :integer, default: 0
    field :status, FlixBackend.Data.CommentStatus, default: :active
    field :is_liked, :boolean, virtual: true, default: false

    belongs_to :product, FlixBackend.Data.Product, type: :binary_id
    belongs_to :user, FlixBackend.Data.User, type: :binary_id, foreign_key: :user_id, references: :uid
    belongs_to :parent, FlixBackend.Data.Comment, type: :binary_id, foreign_key: :parent_id
    belongs_to :root, FlixBackend.Data.Comment, type: :binary_id, foreign_key: :root_id

    has_many :replies, FlixBackend.Data.Comment, foreign_key: :parent_id
    has_many :likes, FlixBackend.Data.CommentLike, foreign_key: :comment_id

    timestamps()
  end

  def changeset(comment, attrs) do
    comment
    |> cast(attrs, [:product_id, :user_id, :parent_id, :root_id, :content, :status])
    |> validate_required([:product_id, :user_id, :content])
    |> validate_length(:content, min: 1, max: 1000)
  end
end
