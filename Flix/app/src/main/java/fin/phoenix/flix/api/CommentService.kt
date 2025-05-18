package fin.phoenix.flix.api

import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.data.CommentContext
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 评论API服务接口
 */
interface CommentService {
    /**
     * 获取商品的评论列表
     */
    @GET("comments/product/{product_id}")
    suspend fun getProductComments(
        @Path("product_id") productId: String,
        @Query("offset") offset: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("current_user_id") currentUserId: String? = null
    ): Response<CommentListResponse>

    /**
     * 获取评论的回复列表
     */
    @GET("comments/{id}/replies")
    suspend fun getCommentReplies(
        @Path("id") commentId: String,
        @Query("offset") offset: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("current_user_id") currentUserId: String? = null
    ): Response<CommentListResponse>

    /**
     * 获取单个评论详情
     */
    @GET("comments/{id}")
    suspend fun getComment(
        @Path("id") commentId: String,
        @Query("current_user_id") currentUserId: String? = null
    ): Response<GenericApiResponse<Comment>>

    /**
     * 获取评论上下文（根评论及所有回复）
     */
    @GET("comments/{id}/context")
    suspend fun getCommentContext(
        @Path("id") commentId: String,
        @Query("current_user_id") currentUserId: String? = null
    ): Response<GenericApiResponse<CommentContext>>

    /**
     * 创建评论
     */
    @POST("comments/product/{product_id}")
    suspend fun createComment(
        @Path("product_id") productId: String,
        @Body request: CreateCommentRequest
    ): Response<GenericApiResponse<Comment>>

    /**
     * 回复评论
     */
    @POST("comments/{comment_id}/reply")
    suspend fun replyToComment(
        @Path("comment_id") commentId: String,
        @Body request: CreateCommentRequest
    ): Response<GenericApiResponse<Comment>>

    /**
     * 点赞评论
     */
    @POST("comments/{id}/like")
    suspend fun likeComment(
        @Path("id") commentId: String
    ): Response<GenericApiResponse<Comment>>

    /**
     * 取消点赞评论
     */
    @DELETE("comments/{id}/like")
    suspend fun unlikeComment(
        @Path("id") commentId: String
    ): Response<GenericApiResponse<Comment>>

    /**
     * 查询是否已点赞评论
     */
    @GET("comments/{id}/liked")
    suspend fun isCommentLiked(
        @Path("id") commentId: String
    ): Response<GenericApiResponse<Boolean>>

    /**
     * 删除评论
     */
    @DELETE("comments/{id}")
    suspend fun deleteComment(
        @Path("id") commentId: String
    ): Response<GenericApiResponse<Unit>>
}

/**
 * 创建评论请求
 */
data class CreateCommentRequest(
    val content: String
)

/**
 * 评论列表响应
 */
data class CommentListResponse(
    val success: Boolean,
    val message: String,
    val data: List<Comment>,
    val totalCount: Int,
    val currentPage: Int,
    val totalPages: Int
)

/**
 * 单个评论响应
 */
data class CommentResponse(
    val success: Boolean,
    val message: String,
    val data: Comment
)

/**
 * 评论上下文响应
 */
data class CommentContextResponse(
    val success: Boolean,
    val message: String,
    val data: CommentContext
)

/**
 * 布尔值响应
 */
data class BooleanResponse(
    val success: Boolean,
    val message: String,
    val data: Boolean
)