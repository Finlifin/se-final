defmodule FlixBackendWeb.CommentController do
  use FlixBackendWeb, :controller

  alias FlixBackend.CommentService
  alias FlixBackend.Data.User
  alias FlixBackend.Guardian
  alias FlixBackendWeb.ApiResponse

  # 获取商品的评论列表
  def list_product_comments(conn, %{"product_id" => product_id} = params) do
    limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1)
    offset = Map.get(params, "offset", "1") |> String.to_integer() |> max(1)
    current_user_id = Map.get(params, "current_user_id", nil)

    # 获取当前用户ID
    # current_user_id = case Guardian.Plug.current_resource(conn) do
    #   nil -> nil
    #   account -> User.get_user_by_uid(account.user_id).uid
    # end


    case CommentService.list_product_comments(product_id, offset, limit, current_user_id) do
      {:ok, comments, total_count} ->
        current_page = div(offset, limit) + 1
        total_pages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

        conn
        |> put_status(:ok)
        |> json(ApiResponse.comment_list_response(comments, total_count, current_page, total_pages))
      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 获取评论的回复列表
  def list_comment_replies(conn, %{"comment_id" => comment_id} = params) do
    limit = Map.get(params, "limit", "10") |> String.to_integer() |> max(1)
    offset = Map.get(params, "offset", "1") |> String.to_integer() |> max(1)
    current_user_id = Map.get(params, "current_user_id", nil)

    # 获取当前用户ID
    # current_user_id = case Guardian.Plug.current_resource(conn) do
    #   nil -> nil
    #   account -> User.get_user_by_uid(account.user_id).uid
    # end

    case CommentService.list_comment_replies(comment_id, offset, limit, current_user_id) do
      {:ok, replies, total_count} ->
        current_page = div(offset, limit) + 1
        total_pages = if total_count > 0, do: ceil(total_count / limit) |> trunc(), else: 1

        conn
        |> put_status(:ok)
        |> json(ApiResponse.comment_list_response(replies, total_count, current_page, total_pages))
      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))
      {:error, reason} ->
        conn
        |> put_status(:bad_request)
        |> json(ApiResponse.error_response(reason))
    end
  end

  # 创建根评论
  def create_comment(conn, %{"product_id" => product_id, "content" => content}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        case CommentService.create_comment(product_id, user.uid, content) do
          {:ok, comment} ->
            conn
            |> put_status(:created)
            |> json(ApiResponse.comment_response(comment, "评论发布成功"))
          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))
          {:error, :validation_error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> json(ApiResponse.validation_error_response(changeset))
        end
    end
  end

  # 回复评论
  def reply_to_comment(conn, %{"comment_id" => comment_id, "content" => content}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        case CommentService.reply_to_comment(comment_id, user.uid, content) do
          {:ok, comment} ->
            conn
            |> put_status(:created)
            |> json(ApiResponse.comment_response(comment, "回复发布成功"))
          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))
        end
    end
  end

  # 获取评论详情
  def get_comment(conn, %{"id" => comment_id} = params) do
    # 获取当前用户ID
    current_user_id = Map.get(params, "current_user_id", nil)
    # current_user_id = case Guardian.Plug.current_resource(conn) do
    #   nil -> nil
    #   account -> User.get_user_by_uid(account.user_id).uid
    # end

    case CommentService.get_comment(comment_id, current_user_id) do
      {:ok, comment} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.comment_response(comment))
      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))
    end
  end

  # 点赞评论
  def like_comment(conn, %{"id" => comment_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        case CommentService.like_comment(comment_id, user.uid) do
          {:ok, comment} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.comment_response(comment, "点赞成功"))
          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))
          {:error, :conflict, message} ->
            conn
            |> put_status(:conflict)
            |> json(ApiResponse.error_response(message))
        end
    end
  end

  # 取消点赞评论
  def unlike_comment(conn, %{"id" => comment_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        case CommentService.unlike_comment(comment_id, user.uid) do
          {:ok, comment} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.comment_response(comment, "取消点赞成功"))
          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))
        end
    end
  end

  # 检查用户是否已点赞评论
  def is_comment_liked(conn, %{"id" => comment_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        case CommentService.is_comment_liked?(comment_id, user.uid) do
          {:ok, is_liked} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.success_response("获取点赞状态成功", is_liked))
        end
    end
  end

  # 获取评论及其上下文（根评论和所有回复）
  def get_comment_with_context(conn, %{"id" => comment_id} = params) do
    # 获取当前用户ID
    current_user_id = Map.get(params, "current_user_id", nil)
    # current_user_id = case Guardian.Plug.current_resource(conn) do
    #   nil -> nil
    #   account -> User.get_user_by_uid(account.user_id).uid
    # end

    case CommentService.get_comment_with_context(comment_id, current_user_id) do
      {:ok, context} ->
        conn
        |> put_status(:ok)
        |> json(ApiResponse.success_response("获取评论上下文成功", context))
      {:error, :not_found, message} ->
        conn
        |> put_status(:not_found)
        |> json(ApiResponse.not_found_response(message))
    end
  end

  # 删除评论
  def delete_comment(conn, %{"id" => comment_id}) do
    case Guardian.Plug.current_resource(conn) do
      nil ->
        conn
        |> put_status(:unauthorized)
        |> json(ApiResponse.unauthorized_response())
      account ->
        user = User.get_user_by_uid(account.user_id)

        case CommentService.delete_comment(comment_id, user.uid) do
          {:ok, _comment} ->
            conn
            |> put_status(:ok)
            |> json(ApiResponse.success_response("评论删除成功"))
          {:error, :not_found, message} ->
            conn
            |> put_status(:not_found)
            |> json(ApiResponse.not_found_response(message))
          {:error, :unauthorized, message} ->
            conn
            |> put_status(:forbidden)
            |> json(ApiResponse.error_response(message))
        end
    end
  end
end
