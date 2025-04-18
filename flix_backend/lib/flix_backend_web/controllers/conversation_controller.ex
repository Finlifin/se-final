defmodule FlixBackendWeb.ConversationController do
  use FlixBackendWeb, :controller

  alias FlixBackend.Data.{Conversation, UserConversation}
  alias FlixBackend.Repo

  # API 响应工具
  alias FlixBackendWeb.ApiResponse

  # 获取用户的所有会话
  def index(conn, _params) do
    user = conn.assigns.current_user
    conversations = Conversation.get_conversations_for_user(user.uid)

    # 同时获取每个会话的未读消息数等信息
    conversations_with_info = Enum.map(conversations, fn conversation ->
      user_conversation = Repo.get_by(UserConversation,
        user_id: user.uid, conversation_id: conversation.conversation_id)

      Map.merge(
        %{conversation: conversation},
        %{
          unread_count: user_conversation.unread_count || 0,
          is_pinned: user_conversation.is_pinned || false,
          is_muted: user_conversation.is_muted || false,
          last_read_message_id: user_conversation.last_read_message_id
        }
      )
    end)

    conn
    |> put_status(:ok)
    |> json(ApiResponse.success_response(conversations_with_info, "获取会话列表成功"))
  end

  # 获取指定会话详情
  def show(conn, %{"id" => conversation_id}) do
    user = conn.assigns.current_user

    conversation = Repo.get_by(Conversation, conversation_id: conversation_id)

    if conversation && Enum.member?(conversation.participant_ids, user.uid) do
      user_conversation = Repo.get_by(UserConversation,
        user_id: user.uid, conversation_id: conversation_id)

      result = %{
        conversation: conversation,
        user_settings: %{
          unread_count: user_conversation.unread_count || 0,
          is_pinned: user_conversation.is_pinned || false,
          is_muted: user_conversation.is_muted || false,
          last_read_message_id: user_conversation.last_read_message_id,
          draft: user_conversation.draft
        }
      }

      conn
      |> put_status(:ok)
      |> json(ApiResponse.success_response(result, "获取会话成功"))
    else
      conn
      |> put_status(:not_found)
      |> json(ApiResponse.not_found_response("会话不存在或无权访问"))
    end
  end

  # 创建新会话
  def create(conn, %{"type" => type, "participant_ids" => participant_ids}) do
    user = conn.assigns.current_user

    # 验证会话类型
    if type not in ["private", "group"] do
      conn
      |> put_status(:bad_request)
      |> json(ApiResponse.error_response("无效的会话类型"))
    else
      # 处理参与者列表
      all_participants = if type == "private" do
        [other_user_id] = participant_ids
        [user.uid, other_user_id] |> Enum.uniq() |> Enum.sort()
      else
        # 群聊，确保当前用户在参与者中
        (participant_ids ++ [user.uid]) |> Enum.uniq()
      end

      # 为私聊生成标准化的conversation_id
      conversation_id = case type do
        "private" ->
          [first_id, second_id] = all_participants
          "private:#{first_id}:#{second_id}"
        "group" ->
          # 对于群聊使用UUID
          "group:#{Ecto.UUID.generate()}"
      end

      # 检查会话是否已存在
      case Repo.get_by(Conversation, conversation_id: conversation_id) do
        nil ->
          # 创建新会话
          conversation_params = %{
            conversation_id: conversation_id,
            type: type,
            participant_ids: all_participants,
            updated_at: DateTime.utc_now()
          }

          # 事务：创建会话并为所有参与者创建用户会话关系
          result = Ecto.Multi.new()
            |> Ecto.Multi.insert(:conversation, Conversation.changeset(%Conversation{}, conversation_params))
            |> create_user_conversations(all_participants, conversation_id)
            |> Repo.transaction()

          case result do
            {:ok, %{conversation: conversation}} ->
              # 返回新建的会话信息
              conn
              |> put_status(:created)
              |> json(ApiResponse.success_response(%{conversation: conversation}, "会话创建成功"))

            {:error, _failed_operation, failed_value, _changes} ->
              conn
              |> put_status(:unprocessable_entity)
              |> json(ApiResponse.error_response("创建会话失败: #{error_messages(failed_value)}"))
          end

        conversation ->
          # 会话已存在，直接返回
          conn
          |> put_status(:ok)
          |> json(ApiResponse.success_response(%{conversation: conversation, already_exists: true}, "会话已存在"))
      end
    end
  end

  # 更新会话设置（用户级别的设置：置顶、静音等）
  def update(conn, %{"id" => conversation_id} = params) do
    user = conn.assigns.current_user

    user_conversation = Repo.get_by(UserConversation,
      user_id: user.uid, conversation_id: conversation_id)

    if user_conversation do
      # 提取可更新的参数
      update_params = params
        |> Map.take(["is_pinned", "is_muted", "draft"])
        |> Enum.reduce(%{}, fn {k, v}, acc -> Map.put(acc, String.to_atom(k), v) end)

      case UserConversation.changeset(user_conversation, update_params) |> Repo.update() do
        {:ok, updated} ->
          conn
          |> put_status(:ok)
          |> json(ApiResponse.success_response(updated, "会话设置更新成功"))

        {:error, changeset} ->
          conn
          |> put_status(:unprocessable_entity)
          |> json(ApiResponse.error_response("更新会话设置失败: #{error_messages(changeset)}"))
      end
    else
      conn
      |> put_status(:not_found)
      |> json(ApiResponse.not_found_response("会话不存在或您不是该会话的成员"))
    end
  end

  # 删除会话（实际上只是将用户从会话中移除或者标记为已删除）
  def delete(conn, %{"id" => conversation_id}) do
    user = conn.assigns.current_user

    conversation = Repo.get_by(Conversation, conversation_id: conversation_id)
    user_conversation = Repo.get_by(UserConversation,
      user_id: user.uid, conversation_id: conversation_id)

    cond do
      # 会话不存在
      is_nil(conversation) ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response("会话不存在"))

      # 已删除会话
      is_nil(user_conversation) ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response(nil, "会话已删除"))

      # 删除会话
      true ->
        Repo.delete(user_conversation)

        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response(nil, "会话已删除"))
    end
  end

  # 将会话标记为已读
  def mark_read(conn, %{"id" => conversation_id, "last_read_message_id" => last_read_message_id}) do
    user = conn.assigns.current_user

    # 查找会话
    conversation = Repo.get_by(Conversation, conversation_id: conversation_id)

    if conversation do
      # 更新用户会话关系中的已读消息ID
      user_conversation = Repo.get_by(UserConversation,
        user_id: user.uid, conversation_id: conversation_id)

      if user_conversation do
        # 更新用户会话的已读状态
        Repo.update(UserConversation.changeset(user_conversation, %{
          last_read_message_id: last_read_message_id,
          unread_count: 0
        }))

        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response(nil, "会话已标记为已读"))
      else
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response("您不是此会话的成员"))
      end
    else
      conn
      |> put_status(:not_found)
      |> json(ApiResponse.not_found_response("会话不存在"))
    end
  end

  # 辅助函数：为多个用户创建会话关系
  defp create_user_conversations(multi, participant_ids, conversation_id) do
    Enum.reduce(participant_ids, multi, fn user_id, multi ->
      user_conversation_params = %{
        user_id: user_id,
        conversation_id: conversation_id,
        unread_count: 0
      }

      Ecto.Multi.insert(
        multi,
        {:user_conversation, user_id},
        UserConversation.changeset(%UserConversation{}, user_conversation_params)
      )
    end)
  end

  # 辅助函数：格式化错误信息
  defp error_messages(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      Enum.reduce(opts, msg, fn {key, value}, acc ->
        String.replace(acc, "%{#{key}}", to_string(value))
      end)
    end)
    |> Enum.map(fn {k, v} -> "#{k} #{v}" end)
    |> Enum.join(", ")
  end
end
