package fin.phoenix.flix.repository

import android.content.Context
import fin.phoenix.flix.api.CommentService
import fin.phoenix.flix.api.CreateCommentRequest
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.data.CommentContext
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 评论仓库类，处理与评论相关的业务逻辑
 */
class CommentRepository(context: Context) {
    private val commentService = RetrofitClient.createService(CommentService::class.java, context)
    private val userManager = UserManager.getInstance(context)
    
    // 获取当前登录用户ID
    private val currentUserId: String?
        get() = userManager.currentUserId.value

    /**
     * 获取商品的评论列表
     */
    suspend fun getProductComments(
        productId: String,
        page: Int = 1,
        limit: Int = 10
    ): Resource<List<Comment>> = withContext(Dispatchers.IO) {
        val response = commentService.getProductComments(
            productId, 
            page, 
            limit, 
            currentUserId
        )
        if (response.isSuccessful) {
            Resource.Success(response.body()?.data ?: emptyList())
        } else {
            Resource.Error("获取评论列表失败: ${response.message()}")
        }
    }

    /**
     * 获取评论的回复列表
     */
    suspend fun getCommentReplies(
        commentId: String,
        page: Int = 1,
        limit: Int = 10
    ): Resource<List<Comment>> = withContext(Dispatchers.IO) {
        val response = commentService.getCommentReplies(
            commentId, 
            page, 
            limit, 
            currentUserId
        )
        if (response.isSuccessful) {
            Resource.Success(response.body()?.data ?: emptyList())
        } else {
            Resource.Error("获取回复列表失败: ${response.message()}")
        }
    }

    /**
     * 获取单个评论详情
     */
    suspend fun getComment(commentId: String): Resource<Comment> = withContext(Dispatchers.IO) {
        commentService.getComment(commentId, currentUserId).toResource("获取评论详情失败")
    }

    /**
     * 获取评论上下文（根评论及所有回复）
     */
    suspend fun getCommentContext(commentId: String): Resource<CommentContext> = withContext(Dispatchers.IO) {
        commentService.getCommentContext(commentId, currentUserId).toResource("获取评论上下文失败")
    }

    /**
     * 创建评论
     */
    suspend fun createComment(productId: String, content: String): Resource<Comment> = withContext(Dispatchers.IO) {
        val request = CreateCommentRequest(content)
        commentService.createComment(productId, request).toResource("发布评论失败")
    }

    /**
     * 回复评论
     */
    suspend fun replyToComment(commentId: String, content: String): Resource<Comment> = withContext(Dispatchers.IO) {
        val request = CreateCommentRequest(content)
        commentService.replyToComment(commentId, request).toResource("回复评论失败")
    }

    /**
     * 点赞评论
     */
    suspend fun likeComment(commentId: String): Resource<Comment> = withContext(Dispatchers.IO) {
        commentService.likeComment(commentId).toResource("点赞失败")
    }

    /**
     * 取消点赞评论
     */
    suspend fun unlikeComment(commentId: String): Resource<Comment> = withContext(Dispatchers.IO) {
        commentService.unlikeComment(commentId).toResource("取消点赞失败")
    }

    /**
     * 检查用户是否已点赞评论
     */
    suspend fun isCommentLiked(commentId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        commentService.isCommentLiked(commentId).toResource("获取点赞状态失败")
    }

    /**
     * 删除评论
     */
    suspend fun deleteComment(commentId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        commentService.deleteComment(commentId).toResource("删除评论失败")
    }
}