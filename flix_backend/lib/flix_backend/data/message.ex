defmodule FlixBackend.Data.Message do
  @moduledoc """
  消息模块。

  消息系统的核心模块，用于处理用户接收的各种类型的消息：
  - 系统消息（通知、公告）
  - 交互类通知
  - 聊天消息
  - 订单相关通知
  - 支付相关通知

  消息分为服务器发送的消息和用户发送的消息：
  - 服务器消息的 sender 值为 nil
  - 用户消息的 sender 值为发送用户的 ID

  message_id 的格式为：
  - 用户发送消息: ${sender_phone_number}-${client_message_uuid}
  - 服务器发送消息: server-${uuid}

  消息内容以数组格式存储在 content 字段中，可以包含多个内容项，每一项都是一个 Map。
  """

  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query

  @primary_key {:id, :binary_id, autogenerate: true}
  @derive {Phoenix.Param, key: :id}
  @derive {Jason.Encoder,
           only: [
             :id,
             :message_id,
             :sender,
             :receiver,
             :content,
             :message_type,
             :read,
             :inserted_at,
             :updated_at
           ]}
  schema "messages" do
    field :message_id, :string
    field :sender, :binary_id
    field :receiver, :binary_id
    field :content, {:array, :map}, default: []
    field :message_type, FlixBackend.Data.MessageType
    field :read, :boolean, default: false

    timestamps()
  end

  @doc """
  消息改变集。

  ## 参数

  - message: 消息结构
  - attrs: 属性映射

  ## 返回

  改变集
  """
  def changeset(message, attrs) do
    message
    |> cast(attrs, [
      :message_id,
      :sender,
      :receiver,
      :content,
      :message_type,
      :read
    ])
    |> validate_required([
      :message_id,
      :receiver,
      :content,
      :message_type
    ])
    # |> validate_format(:message_id, ~r/^(server|\+?[0-9]+)-[a-zA-Z0-9\-_]+$/,
    #    message: "message_id 格式应为 ${sender_phone_number | server}-${client_message_uuid}")
  end

  @doc """
  获取用户的消息列表。

  ## 参数

  - user_id: 用户ID
  - opts: 选项，可以包含：
    - limit: 返回消息的最大数量，默认为50
    - offset: 分页偏移，默认为0
    - message_type: 消息类型筛选

  ## 返回

  消息记录列表
  """
  def get_messages_for_user(user_id, opts \\ []) do
    limit = Keyword.get(opts, :limit, 50)
    offset = Keyword.get(opts, :offset, 0)
    message_type = Keyword.get(opts, :message_type, nil)

    query = from m in __MODULE__,
      where: m.receiver == ^user_id,
      order_by: [desc: m.inserted_at],
      limit: ^limit,
      offset: ^((offset - 1) * limit)

    query = if message_type do
      from m in query, where: m.message_type == ^message_type
    else
      query
    end

    FlixBackend.Repo.all(query)
  end

  @doc """
  标记指定消息为已读。

  ## 参数

  - message_id: 消息ID

  ## 返回

  - {:ok, message} 成功
  - {:error, :not_found} 消息不存在
  - {:error, changeset} 更新失败
  """
  def mark_as_read(message_id) do
    case FlixBackend.Repo.get(__MODULE__, message_id) do
      nil -> {:error, :not_found}
      message ->
        message
        |> changeset(%{read: true, updated_at: DateTime.utc_now()})
        |> FlixBackend.Repo.update()
    end
  end

  @doc """
  标记用户的所有消息为已读。

  ## 参数

  - user_id: 用户ID

  ## 返回

  {更新的消息数量, nil}
  """
  def mark_all_as_read(user_id) do
    from(m in __MODULE__, where: m.receiver == ^user_id and m.read == false)
    |> FlixBackend.Repo.update_all(set: [read: true, updated_at: DateTime.utc_now()])
  end

  @doc """
  计算用户的未读消息数量。

  ## 参数

  - user_id: 用户ID

  ## 返回

  未读消息数量
  """
  def count_unread_messages(user_id) do
    from(m in __MODULE__, where: m.receiver == ^user_id and m.read == false)
    |> FlixBackend.Repo.aggregate(:count)
  end

  @doc """
  按消息类型统计用户未读消息。

  ## 参数

  - user_id: 用户ID

  ## 返回

  %{total: 总未读数, system: 系统消息数, notification: 通知数, chat: 聊天消息数, ...}
  """
  def count_unread_messages_by_type(user_id) do
    query = from m in __MODULE__,
            where: m.receiver == ^user_id and m.read == false,
            group_by: m.message_type,
            select: {m.message_type, count(m.id)}

    results = FlixBackend.Repo.all(query)

    # 初始化默认结果
    default_counts = %{
      total: 0,
      system: 0,
      chat: 0,
      notification: 0
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

  @doc """
  批量标记多条消息为已读。

  ## 参数

  - message_ids: 消息ID列表
  - user_id: 用户ID

  ## 返回

  {更新的消息数量, nil}
  """
  def mark_multiple_as_read(message_ids, user_id) do
    from(m in __MODULE__,
      where: m.id in ^message_ids and m.receiver == ^user_id and m.read == false
    )
    |> FlixBackend.Repo.update_all(set: [read: true, updated_at: DateTime.utc_now()])
  end

  @doc """
  获取指定时间之后的消息，用于同步操作。

  ## 参数

  - user_id: 用户ID
  - timestamp: 时间戳
  - opts: 选项
    - limit: 返回消息的最大数量，默认为100

  ## 返回

  消息记录列表
  """
  def get_messages_since(user_id, timestamp, opts \\ []) do
    limit = Keyword.get(opts, :limit, 100)

    query = from m in __MODULE__,
      where: m.receiver == ^user_id and m.updated_at > ^timestamp and m.read == ^false,
      order_by: [asc: m.inserted_at],
      limit: ^limit

    FlixBackend.Repo.all(query)
  end

  @doc """
  获取指定时间之前的消息，用于历史消息加载。

  ## 参数

  - user_id: 用户ID
  - datetime: 时间点
  - opts: 选项
    - limit: 返回消息的最大数量，默认为50
    - message_type: 消息类型筛选

  ## 返回

  消息记录列表
  """
  def get_messages_before(user_id, datetime, opts \\ []) do
    limit = Keyword.get(opts, :limit, 50)
    message_type = Keyword.get(opts, :message_type, nil)

    query = from m in __MODULE__,
      where: m.receiver == ^user_id and m.inserted_at < ^datetime,
      order_by: [desc: m.inserted_at],
      limit: ^limit

    query = if message_type do
      from m in query, where: m.message_type == ^message_type
    else
      query
    end

    FlixBackend.Repo.all(query)
  end

  @doc """
  根据message_id获取消息。

  ## 参数

  - message_id: 消息唯一标识符

  ## 返回

  消息记录或nil
  """
  def get_by_message_id(message_id) do
    FlixBackend.Repo.get_by(__MODULE__, message_id: message_id)
  end
end
