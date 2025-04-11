defmodule FlixBackend.Data.Conversation do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query
  alias FlixBackend.Repo

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder,
           only: [
             :id,
             :conversation_id,
             :type,
             :participant_ids,
             :last_message_id,
             :last_message_content,
             :last_message_timestamp,
             :updated_at,
             :inserted_at
           ]}
  schema "conversations" do
    field :conversation_id, :string
    field :type, :string  # private, group
    field :participant_ids, {:array, :binary_id}
    field :last_message_id, :binary_id
    field :last_message_content, :string
    field :last_message_timestamp, :utc_datetime_usec

    timestamps()
  end

  def changeset(conversation, attrs) do
    conversation
    |> cast(attrs, [
      :conversation_id,
      :type,
      :participant_ids,
      :last_message_id,
      :last_message_content,
      :last_message_timestamp,
      :updated_at
    ])
    |> validate_required([
      :conversation_id,
      :type,
      :participant_ids
    ])
    |> validate_inclusion(:type, ["private", "group", "system"])
    |> unique_constraint(:conversation_id)
  end

  # 获取或创建私聊会话
  def get_or_create_private_conversation(user_id1, user_id2) do
    # 对用户ID按字典序排序，确保生成一致的conversation_id
    [first_id, second_id] = Enum.sort([user_id1, user_id2])
    conversation_id = "private:#{first_id}:#{second_id}"

    # 查找现有会话
    case Repo.get_by(__MODULE__, conversation_id: conversation_id) do
      nil ->
        # 创建新会话
        params = %{
          conversation_id: conversation_id,
          type: "private",
          participant_ids: [user_id1, user_id2],
          updated_at: DateTime.utc_now()
        }

        # 事务：创建会话并为双方创建用户会话关系
        Ecto.Multi.new()
        |> Ecto.Multi.insert(:conversation, changeset(%__MODULE__{}, params))
        |> Ecto.Multi.run(:user_conversation1, fn repo, %{conversation: conversation} ->
          uc_params = %{
            user_id: user_id1,
            conversation_id: conversation_id,
            unread_count: 0
          }
          repo.insert(FlixBackend.Data.UserConversation.changeset(
            %FlixBackend.Data.UserConversation{}, uc_params))
        end)
        |> Ecto.Multi.run(:user_conversation2, fn repo, %{conversation: conversation} ->
          uc_params = %{
            user_id: user_id2,
            conversation_id: conversation_id,
            unread_count: 0
          }
          repo.insert(FlixBackend.Data.UserConversation.changeset(
            %FlixBackend.Data.UserConversation{}, uc_params))
        end)
        |> Repo.transaction()
        |> case do
          {:ok, %{conversation: conversation}} -> {:ok, conversation}
          {:error, _failed_operation, failed_value, _} -> {:error, failed_value}
        end

      conversation ->
        {:ok, conversation}
    end
  end

  # 获取用户参与的所有会话列表
  def get_conversations_for_user(user_id) do
    query = from c in __MODULE__,
      join: uc in FlixBackend.Data.UserConversation,
      on: c.conversation_id == uc.conversation_id,
      where: uc.user_id == ^user_id,
      order_by: [desc: c.updated_at]

    Repo.all(query)
  end

  # 获取单个会话
  def get_conversation(conversation_id) do
    Repo.get_by(__MODULE__, conversation_id: conversation_id)
  end

  # 确保系统会话存在
  def ensure_system_conversation(conversation_id, type, participant_ids) do
    case Repo.get_by(__MODULE__, conversation_id: conversation_id) do
      nil ->
        # 创建系统会话
        params = %{
          conversation_id: conversation_id,
          type: "system",
          participant_ids: participant_ids,
          updated_at: DateTime.utc_now()
        }

        {:ok, conversation} = changeset(%__MODULE__{}, params)
        |> Repo.insert()

        # 为参与者创建用户会话关系
        Enum.each(participant_ids, fn user_id ->
          uc_params = %{
            user_id: user_id,
            conversation_id: conversation_id,
            unread_count: 0
          }

          FlixBackend.Data.UserConversation.changeset(
            %FlixBackend.Data.UserConversation{}, uc_params)
          |> Repo.insert()
        end)

        conversation

      conversation -> conversation
    end
  end
end
