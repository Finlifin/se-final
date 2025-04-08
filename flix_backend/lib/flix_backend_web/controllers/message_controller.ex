defmodule FlixBackendWeb.MessageController do
  use FlixBackendWeb, :controller
  import Ecto.Query, only: [from: 2]
  alias FlixBackend.Data.User
  alias FlixBackend.Data.Message
  alias FlixBackend.Guardian
  alias FlixBackendWeb.ApiResponse # Import ApiResponse

  # 获取用户的消息列表（支持分页、消息类型和状态过滤）
  # GET /api/v1/messages?limit=20&offset=0&message_type=private_message&status=unread
  def index(conn, params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse

      account ->
        user = User.get_user_by_uid(account.user_id)

        limit = Map.get(params, "limit", "50") |> String.to_integer()
        offset = Map.get(params, "offset", "0") |> String.to_integer()
        status = Map.get(params, "status")
        message_type = Map.get(params, "message_type")

        opts = [limit: limit, offset: offset]
        opts = if status, do: Keyword.put(opts, :status, String.to_atom(status)), else: opts
        opts = if message_type, do: Keyword.put(opts, :message_type, String.to_atom(message_type)), else: opts

        messages = Message.get_messages_for_user(user.uid, opts)

        conn
        |> put_status(:ok)
        |> json(ApiResponse.message_list_response(messages)) # Use ApiResponse
    end
  end

  # 获取单个消息
  def show(conn, %{"id" => id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse
      account ->
        user = User.get_user_by_uid(account.user_id)

        case FlixBackend.Repo.get(Message, id) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("消息未找到")) # Use ApiResponse

          message ->
            if message.receiver_id == user.uid do
              conn
              |> put_status(:ok)
              |> json(ApiResponse.message_response(message)) # Use ApiResponse
            else
              conn
              |> put_status(:forbidden)
              |> json(ApiResponse.forbidden_response("您无权查看此消息")) # Use ApiResponse
            end
        end
    end
  end

  # 创建新消息
  def create(conn, params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse
      account ->
        user = User.get_user_by_uid(account.user_id)

        message_params = Map.merge(params, %{
          "sender_id" => user.uid,
          "created_at" => DateTime.utc_now(),
          "updated_at" => DateTime.utc_now()
        })

        changeset = Message.changeset(%Message{}, message_params)

        case FlixBackend.Repo.insert(changeset) do
          {:ok, message} ->
            # 发布消息到PubSub
            FlixBackendWeb.Endpoint.broadcast!(
              "user:#{message.receiver_id}",
              "new_message",
              %{message: message}
            )

            conn
            |> put_status(:created)
            |> json(ApiResponse.message_response(message, "消息创建成功")) # Use ApiResponse

          {:error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.validation_error_response(changeset)) # Use ApiResponse
        end
    end
  end

  # 标记消息为已读
  def mark_as_read(conn, %{"id" => id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse
      account ->
        user = User.get_user_by_uid(account.user_id)

        case FlixBackend.Repo.get(Message, id) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("消息未找到")) # Use ApiResponse

          message ->
            if message.receiver_id == user.uid do
              case Message.mark_as_read(id) do
                {:ok, updated_message} ->
                  # 广播状态更新
                  FlixBackendWeb.Endpoint.broadcast!(
                    "user:#{user.uid}",
                    "message_status_changed",
                    %{message_id: id, status: "read"}
                  )

                  conn
                  |> put_status(:ok)
                  |> json(ApiResponse.message_response(updated_message, "消息已标记为已读")) # Use ApiResponse

                {:error, _reason} ->
                  conn
                  |> put_status(:internal_server_error)
                  |> json(ApiResponse.internal_server_error_response("标记消息为已读失败")) # Use ApiResponse
              end
            else
              conn
              |> put_status(:forbidden)
              |> json(ApiResponse.forbidden_response("您无权修改此消息")) # Use ApiResponse
            end
        end
    end
  end

  # 标记所有消息为已读
  def mark_all_as_read(conn, _params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse
      account ->
        user = User.get_user_by_uid(account.user_id)

        {count, _} = Message.mark_all_as_read(user.uid)

        if count > 0 do
          # 广播未读数更新
          broadcast_unread_count_update(user.uid)
        end

        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("所有消息已标记为已读", %{marked_count: count})) # Use ApiResponse
    end
  end

  # 批量标记消息为已读
  # PUT /api/v1/messages/batch_read
  # 请求体: {"message_ids": ["uuid1", "uuid2", ...]}
  def batch_read(conn, %{"message_ids" => ids}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse
      account ->
        user = User.get_user_by_uid(account.user_id)

        # 验证所有消息ID为有效UUID格式
        case validate_uuids(ids) do
          :ok ->
            # 调用批量标记函数
            {count, _} = Message.mark_multiple_as_read(ids, user.uid)

            if count > 0 do
              # 广播未读数更新
              broadcast_unread_count_update(user.uid)
            end

            conn
            |> put_status(:ok)
            |> json(ApiResponse.success_response("批量标记成功", %{marked_count: count})) # Use ApiResponse

          {:error, invalid_ids} ->
            conn
            |> put_status(:bad_request)
            |> json(ApiResponse.error_response("无效的消息 ID 格式" <> inspect(%{invalid_ids: invalid_ids}))) # Use ApiResponse with data
        end
    end
  end

  # 获取未读消息数量，按类型分类
  # GET /api/v1/messages/unread_count
  def unread_count(conn, _params) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse

      account ->
        user = User.get_user_by_uid(account.user_id)
        counts = Message.count_unread_messages_by_type(user.uid)

        conn
        |> put_status(:ok)
        |> json(ApiResponse.message_count_response(counts)) # Use ApiResponse
    end
  end

  # 删除消息
  def delete(conn, %{"id" => id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse
      account ->
        user = User.get_user_by_uid(account.user_id)

        case FlixBackend.Repo.get(Message, id) do
          nil ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response("消息未找到")) # Use ApiResponse

          message ->
            if message.receiver_id == user.uid do
              case FlixBackend.Repo.delete(message) do
                {:ok, _} ->
                  conn
                  |> put_status(:ok) # Use 200 OK with success message instead of 204
                  |> json(ApiResponse.success_response("消息删除成功")) # Use ApiResponse

                {:error, _} ->
                  conn
                  |> put_status(:internal_server_error)
                  |> json(ApiResponse.internal_server_error_response("删除消息失败")) # Use ApiResponse
              end
            else
              conn
              |> put_status(:forbidden)
              |> json(ApiResponse.forbidden_response("您无权删除此消息")) # Use ApiResponse
            end
        end
    end
  end

  # 消息同步端点
  def sync_messages(conn, %{"last_sync_time" => last_sync_time, "user_id" => user_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response()) # Use ApiResponse
      account ->
        if account.user_id != user_id do
          conn
          |> put_status(:forbidden)
          |> json(ApiResponse.forbidden_response("无权同步其他用户的消息"))
        else
          case DateTime.from_iso8601(last_sync_time) do
            {:ok, datetime, _} ->
              # 获取上次同步后的新消息
              query = from m in Message,
                where: m.receiver_id == ^user_id and m.updated_at > ^datetime,
                order_by: [asc: m.created_at]

              messages = FlixBackend.Repo.all(query)
              sync_time = DateTime.utc_now() |> DateTime.to_iso8601()

              conn
              |> put_status(:ok)
              |> json(ApiResponse.message_sync_response(messages, sync_time)) # Use ApiResponse

            {:error, _} ->
              conn
              |> put_status(:bad_request)
              |> json(ApiResponse.error_response("无效的时间戳格式")) # Use ApiResponse
          end
        end
    end
  end

  # 辅助函数：验证ID列表是否为有效UUID
  defp validate_uuids(ids) do
    invalid_ids = Enum.filter(ids, fn id ->
      case Ecto.UUID.cast(id) do
        {:ok, _} -> false
        :error -> true
      end
    end)

    if Enum.empty?(invalid_ids) do
      :ok
    else
      {:error, invalid_ids}
    end
  end

  # 辅助函数：广播未读数更新
  defp broadcast_unread_count_update(user_id) do
    # 获取最新的未读数统计
    counts = Message.count_unread_messages_by_type(user_id)

    # 通过PubSub广播最新未读数
    FlixBackendWeb.Endpoint.broadcast!(
      "user:#{user_id}",
      "unread_count_update",
      %{counts: counts}
    )

    :ok
  end
end
