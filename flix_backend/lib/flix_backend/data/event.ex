defmodule FlixBackend.Data.Event do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder,
           only: [
             :id,
             :event_type,
             :payload,
             :event_timestamp,
             :target_user_id,
             :inserted_at
           ]}
  schema "events" do
    field :event_type, :string
    field :payload, :map
    field :event_timestamp, :utc_datetime_usec
    field :target_user_id, :binary_id

    timestamps()
  end

  def changeset(event, attrs) do
    event
    |> cast(attrs, [
      :event_type,
      :payload,
      :event_timestamp,
      :target_user_id
    ])
    |> validate_required([
      :event_type,
      :payload,
      :event_timestamp,
      :target_user_id
    ])
    |> validate_inclusion(:event_type, ["new_message", "message_status_update", "message_recalled"])
  end

  # 获取用户在指定时间戳后的所有事件
  def get_events_since(user_id, timestamp, limit \\ 100) do
    from(e in __MODULE__,
      where: e.target_user_id == ^user_id and e.event_timestamp > ^timestamp,
      order_by: [asc: e.event_timestamp],
      limit: ^limit
    )
    |> FlixBackend.Repo.all()
  end

  # 创建新消息事件
  def create_new_message_event(message, target_user_id) do
    params = %{
      event_type: "new_message",
      payload: message,
      event_timestamp: DateTime.utc_now(),
      target_user_id: target_user_id
    }

    changeset(%__MODULE__{}, params)
    |> FlixBackend.Repo.insert()
  end

  # 创建消息状态更新事件
  def create_status_update_event(message_id, conversation_id, status, target_user_id) do
    params = %{
      event_type: "message_status_update",
      payload: %{
        message_id: message_id,
        conversation_id: conversation_id,
        status: status,
        updated_at: DateTime.utc_now()
      },
      event_timestamp: DateTime.utc_now(),
      target_user_id: target_user_id
    }

    changeset(%__MODULE__{}, params)
    |> FlixBackend.Repo.insert()
  end

  # 创建消息撤回事件
  def create_recall_event(message_id, conversation_id, target_user_id) do
    params = %{
      event_type: "message_recalled",
      payload: %{
        message_id: message_id,
        conversation_id: conversation_id,
        status: "withdrawn",
        updated_at: DateTime.utc_now()
      },
      event_timestamp: DateTime.utc_now(),
      target_user_id: target_user_id
    }

    changeset(%__MODULE__{}, params)
    |> FlixBackend.Repo.insert()
  end
end
