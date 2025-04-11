defmodule FlixBackend.Data.UserConversation do
  use Ecto.Schema
  import Ecto.Changeset
  alias FlixBackend.Repo

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder,
           only: [
             :id,
             :user_id,
             :conversation_id,
             :last_read_message_id,
             :unread_count,
             :is_pinned,
             :is_muted,
             :draft,
             :inserted_at,
             :updated_at
           ]}
  schema "user_conversations" do
    field :user_id, :binary_id
    field :conversation_id, :string
    field :last_read_message_id, :binary_id
    field :unread_count, :integer, default: 0
    field :is_pinned, :boolean, default: false
    field :is_muted, :boolean, default: false
    field :draft, :string

    timestamps()
  end

  def changeset(user_conversation, attrs) do
    user_conversation
    |> cast(attrs, [
      :user_id,
      :conversation_id,
      :last_read_message_id,
      :unread_count,
      :is_pinned,
      :is_muted,
      :draft
    ])
    |> validate_required([
      :user_id,
      :conversation_id
    ])
    |> unique_constraint([:user_id, :conversation_id])
  end

  # 增加未读消息计数
  def increment_unread(user_conversation) do
    user_conversation
    |> changeset(%{unread_count: user_conversation.unread_count + 1})
    |> Repo.update()
  end

  # 获取用户的会话设置
  def get_conversation_settings(user_id, conversation_id) do
    Repo.get_by(__MODULE__, user_id: user_id, conversation_id: conversation_id)
  end

  # 更新会话设置
  def update_settings(user_id, conversation_id, settings) do
    case Repo.get_by(__MODULE__, user_id: user_id, conversation_id: conversation_id) do
      nil -> {:error, :not_found}
      user_conversation ->
        user_conversation
        |> changeset(settings)
        |> Repo.update()
    end
  end

  # 获取用户的所有会话设置
  def list_for_user(user_id) do
    import Ecto.Query

    query = from uc in __MODULE__,
      where: uc.user_id == ^user_id,
      order_by: [desc: uc.updated_at]

    Repo.all(query)
  end
end
