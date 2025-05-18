defmodule FlixBackend.Data.CommentLike do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder, only: [:id, :comment_id, :user_id, :inserted_at]}

  schema "comment_likes" do
    belongs_to :comment, FlixBackend.Data.Comment, type: :binary_id
    belongs_to :user, FlixBackend.Data.User, type: :binary_id, foreign_key: :user_id, references: :uid

    timestamps()
  end

  def changeset(comment_like, attrs) do
    comment_like
    |> cast(attrs, [:comment_id, :user_id])
    |> validate_required([:comment_id, :user_id])
    |> unique_constraint([:comment_id, :user_id], name: "comment_likes_comment_id_user_id_index")
  end
end
