defmodule FlixBackend.Data.Message do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder,
           only: [
             :id,
             :client_message_id,
             :conversation_id,
             :sender_id,
             :receiver_id,
             :content,
             :message_type,
             :status,
             :reference_id,
             :server_timestamp,
             :client_timestamp,
             :inserted_at,
             :updated_at
           ]}
  schema "messages" do
    field :client_message_id, :binary_id
    field :conversation_id, :string
    field :sender_id, :binary_id
    field :receiver_id, :binary_id
    field :content, {:array, :map}, default: []
    field :message_type, FlixBackend.Data.MessageType
    field :status, FlixBackend.Data.MessageStatus, default: :unread
    field :reference_id, :binary_id
    field :server_timestamp, :utc_datetime_usec
    field :client_timestamp, :utc_datetime_usec

    timestamps()
  end

  def changeset(message, attrs) do
    message
    # 过滤掉不需要的字段
    |> cast(attrs, [
      :client_message_id,
      :conversation_id,
      :sender_id,
      :receiver_id,
      :content,
      :message_type,
      :status,
      :reference_id,
      :server_timestamp,
      :client_timestamp
    ])
    |> validate_required([
      :content,
      :message_type
    ])
  end

  def get_messages_for_user(user_id, opts \\ []) do
    limit = Keyword.get(opts, :limit, 50)
    offset = Keyword.get(opts, :offset, 0)
    status = Keyword.get(opts, :status, nil)
    message_type = Keyword.get(opts, :message_type, nil)

    query = from m in __MODULE__,
      where: m.receiver_id == ^user_id,
      order_by: [desc: m.inserted_at],
      limit: ^limit,
      offset: ^offset

    query = if status do
      from m in query, where: m.status == ^status
    else
      query
    end

    query = if message_type do
      from m in query, where: m.message_type == ^message_type
    else
      query
    end

    FlixBackend.Repo.all(query)
  end

  def mark_as_read(message_id) do
    case FlixBackend.Repo.get(__MODULE__, message_id) do
      nil -> {:error, :not_found}
      message ->
        message
        |> changeset(%{status: :read, updated_at: DateTime.utc_now()})
        |> FlixBackend.Repo.update()
    end
  end

  def mark_all_as_read(user_id) do
    from(m in __MODULE__, where: m.receiver_id == ^user_id and m.status == :unread)
    |> FlixBackend.Repo.update_all(set: [status: :read, updated_at: DateTime.utc_now()])
  end

  def count_unread_messages(user_id) do
    from(m in __MODULE__, where: m.receiver_id == ^user_id and m.status == :unread)
    |> FlixBackend.Repo.aggregate(:count)
  end

  # 按消息类型统计未读消息
  def count_unread_messages_by_type(user_id) do
    query = from m in __MODULE__,
            where: m.receiver_id == ^user_id and m.status == :unread,
            group_by: m.message_type,
            select: {m.message_type, count(m.id)}

    results = FlixBackend.Repo.all(query)

    # 初始化默认结果
    default_counts = %{
      total: 0,
      system_notification: 0,
      system_announcement: 0,
      interaction: 0,
      private_message: 0
    }

    # 计算总数并填充各类型数量
    counts_by_type = Enum.reduce(results, default_counts, fn {type, count}, acc ->
      # 将Atom转为String作为map的key
      Map.put(acc, type, count)
    end)

    # 计算总数
    total = Enum.reduce(results, 0, fn {_type, count}, acc -> acc + count end)

    # 更新总数
    Map.put(counts_by_type, :total, total)
  end

  # 批量标记消息已读
  def mark_multiple_as_read(message_ids, user_id) do
    from(m in __MODULE__,
      where: m.id in ^message_ids and m.receiver_id == ^user_id and m.status == :unread
    )
    |> FlixBackend.Repo.update_all(set: [status: :read, updated_at: DateTime.utc_now()])
  end

  # 获取指定时间之后的消息（用于同步）
  def get_messages_since(user_id, timestamp, opts \\ []) do
    limit = Keyword.get(opts, :limit, 100)

    query = from m in __MODULE__,
      where: m.receiver_id == ^user_id and m.updated_at > ^timestamp,
      order_by: [asc: m.inserted_at],
      limit: ^limit

    FlixBackend.Repo.all(query)
  end

  # 获取指定时间之前的消息
  def get_messages_before(user_id, datetime, opts \\ []) do
    limit = Keyword.get(opts, :limit, 50)
    message_type = Keyword.get(opts, :message_type, nil)

    query = from m in __MODULE__,
      where: m.receiver_id == ^user_id and m.inserted_at < ^datetime,
      order_by: [desc: m.inserted_at],
      limit: ^limit

    query = if message_type do
      from m in query, where: m.message_type == ^message_type
    else
      query
    end

    FlixBackend.Repo.all(query)
  end

  # 获取会话的消息
  def get_conversation_messages(conversation_id, opts \\ []) do
    limit = Keyword.get(opts, :limit, 50)
    before_timestamp = Keyword.get(opts, :before_timestamp, nil)

    query = from m in __MODULE__,
      where: m.conversation_id == ^conversation_id,
      order_by: [desc: m.server_timestamp],
      limit: ^limit

    query = if before_timestamp do
      from m in query, where: m.server_timestamp < ^before_timestamp
    else
      query
    end

    FlixBackend.Repo.all(query)
  end

  # 撤回消息
  def withdraw_message(message_id, user_id) do
    case FlixBackend.Repo.get_by(__MODULE__, id: message_id, sender_id: user_id) do
      nil -> {:error, :not_found}
      message ->
        # 检查是否在可撤回时间范围内（例如2分钟）
        time_diff = DateTime.diff(DateTime.utc_now(), message.server_timestamp, :second)

        if time_diff <= 120 do
          # 替换内容为占位符
          withdrawn_content = %{
            "text" => "此消息已被撤回",
            "original_type" => Atom.to_string(message.message_type),
          }

          message
          |> changeset(%{
            status: :withdrawn,
            content: withdrawn_content,
            updated_at: DateTime.utc_now()
          })
          |> FlixBackend.Repo.update()
        else
          {:error, :time_expired}
        end
    end
  end

  # 清空会话中的所有消息
  def clear_conversation_messages(conversation_id) do
    from(m in __MODULE__, where: m.conversation_id == ^conversation_id)
    |> FlixBackend.Repo.delete_all()
  end

  # 根据客户端消息ID获取消息
  def get_by_client_message_id(client_message_id) do
    FlixBackend.Repo.get_by(__MODULE__, client_message_id: client_message_id)
  end
end
