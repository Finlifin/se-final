defmodule FlixBackend.CommentService do
  @moduledoc """
  提供评论系统的核心服务功能，包括创建评论、回复、点赞等
  """

  import Ecto.Query, warn: false
  alias FlixBackend.Repo
  alias FlixBackend.Data.Comment
  alias FlixBackend.Data.CommentLike
  alias FlixBackend.Data.User
  alias FlixBackend.Data.Product
  alias FlixBackend.Messaging

  @doc """
  获取商品的评论列表，支持分页，并标记当前用户是否点赞
  """
  def list_product_comments(product_id, offset \\ 1, limit \\ 10, current_user_id \\ nil) do
    # 仅查询根评论（即parent_id为null的评论）
    query =
      from c in Comment,
        where: c.product_id == ^product_id and is_nil(c.parent_id) and c.status == :active,
        order_by: [desc: c.inserted_at],
        preload: [:user]

    # 添加分页
    page_offset = (offset - 1) * limit

    query =
      from c in query,
        limit: ^limit,
        offset: ^page_offset

    # 执行查询
    comments = Repo.all(query)

    # 计算总数
    count_query =
      from c in Comment,
        where: c.product_id == ^product_id and is_nil(c.parent_id) and c.status == :active,
        select: count(c.id)

    total_count = Repo.one(count_query)

    # 如果提供了当前用户ID，标记用户是否点赞了每条评论
    comments = if current_user_id do
      Enum.map(comments, fn comment ->
        {:ok, liked} = is_comment_liked?(comment.id, current_user_id)
        # 精简用户信息
        simplified_user = simplify_user_info(comment.user)
        # 使用 Map.put 而不是结构体更新语法，因为 is_liked 不是 Comment 结构体的字段
        comment
        |> Map.put(:is_liked, liked)
        |> Map.put(:user, simplified_user)
      end)
    else
      # 仅精简用户信息
      Enum.map(comments, fn comment ->
        simplified_user = simplify_user_info(comment.user)
        Map.put(comment, :user, simplified_user)
      end)
    end

    {:ok, comments, total_count}
  end

  @doc """
  获取评论的回复列表，支持分页，并标记当前用户是否点赞
  """
  def list_comment_replies(comment_id, offset \\ 1, limit \\ 10, current_user_id \\ nil) do
    # 查询评论是否存在
    case Repo.get(Comment, comment_id) do
      nil ->
        {:error, :not_found, "评论不存在"}
      comment ->
        # 查询所有回复
        query =
          from c in Comment,
            where: c.parent_id == ^comment.id and c.status == :active,
            order_by: [asc: c.inserted_at],
            preload: [:user]

        # 添加分页
        page_offset = (offset - 1) * limit

        query =
          from c in query,
            limit: ^limit,
            offset: ^page_offset

        # 执行查询
        replies = Repo.all(query)

        # 计算总数
        count_query =
          from c in Comment,
            where: c.parent_id == ^comment.id and c.status == :active,
            select: count(c.id)

        total_count = Repo.one(count_query)

        # 如果提供了当前用户ID，标记用户是否点赞了每条评论
        replies = if current_user_id do
          Enum.map(replies, fn reply ->
            {:ok, liked} = is_comment_liked?(reply.id, current_user_id)
            simplified_user = simplify_user_info(reply.user)
            # 使用 Map.put 替代结构体更新语法
            reply
            |> Map.put(:is_liked, liked)
            |> Map.put(:user, simplified_user)
          end)
        else
          Enum.map(replies, fn reply ->
            simplified_user = simplify_user_info(reply.user)
            Map.put(reply, :user, simplified_user)
          end)
        end

        {:ok, replies, total_count}
    end
  end

  @doc """
  获取单个评论详情，并标记当前用户是否已点赞
  """
  def get_comment(comment_id, current_user_id \\ nil) do
    case Repo.get(Comment, comment_id) do
      nil ->
        {:error, :not_found, "评论不存在"}
      comment ->
        comment = Repo.preload(comment, [:user])

        # 如果提供了当前用户ID，标记是否已点赞
        comment = if current_user_id do
          {:ok, liked} = is_comment_liked?(comment.id, current_user_id)
          simplified_user = simplify_user_info(comment.user)
          # 使用 Map.put 替代结构体更新语法
          comment
          |> Map.put(:is_liked, liked)
          |> Map.put(:user, simplified_user)
        else
          # 仅精简用户信息
          simplified_user = simplify_user_info(comment.user)
          Map.put(comment, :user, simplified_user)
        end

        {:ok, comment}
    end
  end

  @doc """
  创建根评论
  """
  def create_comment(product_id, user_id, content) do
    # 检查商品是否存在
    case Repo.get(Product, product_id) do
      nil ->
        {:error, :not_found, "商品不存在"}
      product ->
        # 构造评论参数
        comment_params = %{
          product_id: product_id,
          user_id: user_id,
          content: content
        }

        # 创建评论
        %Comment{}
        |> Comment.changeset(comment_params)
        |> Repo.insert()
        |> case do
          {:ok, comment} ->
            # 发送评论通知给商品卖家
            if product.seller_id != user_id do
              send_comment_notification(comment, product)
            end

            comment = Repo.preload(comment, [:user])
            {:ok, comment}
          {:error, changeset} ->
            {:error, :validation_error, changeset}
        end
    end
  end

  @doc """
  回复评论
  """
  def reply_to_comment(comment_id, user_id, content) do
    # 检查父评论是否存在
    case Repo.get(Comment, comment_id) do
      nil ->
        {:error, :not_found, "评论不存在"}
      parent_comment ->
        # 确定根评论ID
        root_id = if parent_comment.root_id, do: parent_comment.root_id, else: parent_comment.id

        # 构造回复参数
        reply_params = %{
          product_id: parent_comment.product_id,
          user_id: user_id,
          parent_id: comment_id,
          root_id: root_id,
          content: content
        }

        # 创建回复
        {:ok, reply} =
          %Comment{}
          |> Comment.changeset(reply_params)
          |> Repo.insert()

        # 更新父评论的回复计数
        parent_comment
        |> Ecto.Changeset.change(%{replies_count: parent_comment.replies_count + 1})
        |> Repo.update()

        # 更新根评论的回复计数（如果不是直接回复根评论）
        if parent_comment.id != root_id do
          from(c in Comment, where: c.id == ^root_id)
          |> Repo.update_all(inc: [replies_count: 1])
        end

        # 发送评论回复通知给原评论作者
        if parent_comment.user_id != user_id do
          product = Repo.get(Product, parent_comment.product_id)
          send_reply_notification(reply, parent_comment, product)
        end

        # 加载评论用户信息
        reply = Repo.preload(reply, [:user])

        {:ok, reply}
    end
  end

  @doc """
  点赞评论
  """
  def like_comment(comment_id, user_id) do
    case Repo.get(Comment, comment_id) do
      nil ->
        {:error, :not_found, "评论不存在"}
      comment ->
        # 检查用户是否已经点赞
        query =
          from l in CommentLike,
            where: l.comment_id == ^comment_id and l.user_id == ^user_id

        already_liked = Repo.exists?(query)

        if already_liked do
          {:error, :conflict, "已经点赞过此评论"}
        else
          # 创建点赞记录
          like_params = %{
            comment_id: comment_id,
            user_id: user_id
          }

          {:ok, _like} =
            %CommentLike{}
            |> CommentLike.changeset(like_params)
            |> Repo.insert()

          # 更新评论点赞计数
          {1, _} =
            from(c in Comment, where: c.id == ^comment_id)
            |> Repo.update_all(inc: [likes_count: 1])

          # 重新获取更新后的评论，并预加载用户关联
          updated_comment =
            Comment
            |> Repo.get(comment_id)
            |> Repo.preload([:user])

          # 发送点赞通知
          if comment.user_id != user_id do
            send_like_notification(updated_comment, user_id)
          end

          {:ok, updated_comment}
        end
    end
  end

  @doc """
  取消点赞评论
  """
  def unlike_comment(comment_id, user_id) do
    case Repo.get(Comment, comment_id) do
      nil ->
        {:error, :not_found, "评论不存在"}
      _comment ->
        # 查找并删除点赞记录
        query =
          from l in CommentLike,
            where: l.comment_id == ^comment_id and l.user_id == ^user_id

        case Repo.delete_all(query) do
          {0, _} ->
            {:error, :not_found, "未找到点赞记录"}
          {_, _} ->
            # 更新评论点赞计数
            {1, _} =
              from(c in Comment, where: c.id == ^comment_id)
              |> Repo.update_all(inc: [likes_count: -1])

            # 重新获取更新后的评论，并预加载用户关联
            updated_comment =
              Comment
              |> Repo.get(comment_id)
              |> Repo.preload([:user])

            {:ok, updated_comment}
        end
    end
  end

  @doc """
  检查用户是否已点赞评论
  """
  def is_comment_liked?(comment_id, user_id) do
    query =
      from l in CommentLike,
        where: l.comment_id == ^comment_id and l.user_id == ^user_id

    liked = Repo.exists?(query)
    {:ok, liked}
  end

  @doc """
  获取评论所在的根评论和所有回复，并标记当前用户是否已点赞
  """
  def get_comment_with_context(comment_id, current_user_id \\ nil) do
    case Repo.get(Comment, comment_id) do
      nil ->
        {:error, :not_found, "评论不存在"}
      comment ->
        # 如果是子评论，找到它的根评论
        root_id = if comment.root_id, do: comment.root_id, else: comment.id

        # 查询根评论
        root_comment =
          Comment
          |> Repo.get(root_id)
          |> Repo.preload([:user])

        # 处理根评论，添加点赞信息和精简用户数据
        root_comment = if current_user_id do
          {:ok, liked} = is_comment_liked?(root_comment.id, current_user_id)
          simplified_user = simplify_user_info(root_comment.user)
          # 使用 Map.put 替代结构体更新语法
          root_comment
          |> Map.put(:is_liked, liked)
          |> Map.put(:user, simplified_user)
        else
          simplified_user = simplify_user_info(root_comment.user)
          Map.put(root_comment, :user, simplified_user)
        end

        # 查询根评论下的所有回复（按时间顺序）
        replies_query =
          from c in Comment,
            where: c.root_id == ^root_id and c.status == :active,
            order_by: [asc: c.inserted_at],
            preload: [:user]

        replies = Repo.all(replies_query)

        # 处理所有回复，添加点赞信息和精简用户数据
        replies = if current_user_id do
          Enum.map(replies, fn reply ->
            {:ok, liked} = is_comment_liked?(reply.id, current_user_id)
            simplified_user = simplify_user_info(reply.user)
            # 使用 Map.put 替代结构体更新语法
            reply
            |> Map.put(:is_liked, liked)
            |> Map.put(:user, simplified_user)
          end)
        else
          Enum.map(replies, fn reply ->
            simplified_user = simplify_user_info(reply.user)
            Map.put(reply, :user, simplified_user)
          end)
        end

        {:ok, %{root_comment: root_comment, replies: replies, target_comment_id: comment_id}}
    end
  end

  @doc """
  删除评论（软删除，更改状态为deleted）
  """
  def delete_comment(comment_id, user_id) do
    case Repo.get(Comment, comment_id) do
      nil ->
        {:error, :not_found, "评论不存在"}
      comment ->
        if comment.user_id != user_id do
          {:error, :unauthorized, "没有权限删除此评论"}
        else
          comment
          |> Ecto.Changeset.change(%{status: :deleted})
          |> Repo.update()
        end
    end
  end

  # 私有函数 - 发送评论通知
  defp send_comment_notification(comment, product) do
    # 查找商品所有者
    seller = Repo.get(User, product.seller_id)
    # 查找评论所有者
    commenter = Repo.get(User, comment.user_id)

    if seller && commenter do
      # 准备消息内容
      text_content = "用户 #{commenter.user_name} 评论了你的商品 '#{product.title}'"

      # 预加载评论的用户关联
      comment = Repo.preload(comment, [:user])

      # 构造消息体
      content = [
        %{type: "text", payload: text_content},
        %{type: "product", payload: product},
        %{type: "comment", payload: comment}
      ]

      # 发送系统消息
      Messaging.send_private_message(
        commenter.uid,
        seller.uid,
        content,
        "#{commenter.uid}-comment-#{DateTime.utc_now() |> DateTime.to_unix(:millisecond)}"
      )
    end
  end

  # 私有函数 - 发送评论回复通知
  defp send_reply_notification(reply, parent_comment, product) do
    # 查找原评论所有者
    parent_commenter = Repo.get(User, parent_comment.user_id)
    # 查找回复所有者
    replier = Repo.get(User, reply.user_id)

    if parent_commenter && replier do
      # 准备消息内容
      text_content = "用户 #{replier.user_name} 回复了你在商品 '#{product.title}' 下的评论"

      # 预加载回复的用户关联
      reply = Repo.preload(reply, [:user])

      # 构造消息体
      content = [
        %{type: "text", payload: text_content},
        %{type: "product", payload: product},
        %{type: "comment", payload: reply}
      ]

      # 发送系统消息
      Messaging.send_private_message(
        replier.uid,
        parent_commenter.uid,
        content,
        "#{replier.uid}-reply-#{DateTime.utc_now() |> DateTime.to_unix(:millisecond)}"
      )
    end
  end

  # 私有函数 - 发送点赞通知
  defp send_like_notification(comment, liker_id) do
    # 查找评论所有者
    comment_owner = Repo.get(User, comment.user_id)
    # 查找点赞用户
    liker = Repo.get(User, liker_id)
    # 查找相关商品
    product = Repo.get(Product, comment.product_id)

    if comment_owner && liker && product do
      # 准备消息内容
      text_content = "用户 #{liker.user_name} 赞了你在商品 '#{product.title}' 下的评论"

      # 预加载评论的用户关联
      comment = Repo.preload(comment, [:user])

      # 构造消息体
      content = [
        %{type: "text", payload: text_content},
        %{type: "product", payload: product},
        %{type: "like", payload: %{comment_id: comment.id}}
      ]

      # 发送系统消息
      Messaging.send_private_message(
        liker.uid,
        comment_owner.uid,
        content,
        "#{liker.uid}-like-#{DateTime.utc_now() |> DateTime.to_unix(:millisecond)}"
      )
    end
  end

  # 私有函数 - 精简用户信息
  defp simplify_user_info(user) do
    %{
      id: user.uid,
      user_name: user.user_name,
      avatar_url: user.avatar_url
    }
  end
end
