package fin.phoenix.flix.api

import fin.phoenix.flix.data.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 会话管理相关API接口
 */
interface ConversationService {
    /**
     * 获取当前用户的所有会话列表
     */
    @GET("api/v1/conversations")
    suspend fun getConversations(): Response<GenericApiResponse<List<ConversationListItem>>>

    /**
     * 获取指定会话的详细信息
     * @param id 会话ID
     */
    @GET("api/v1/conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): Response<GenericApiResponse<ConversationDetail>>

    /**
     * 创建一个新的会话
     * @param request 创建会话的请求参数
     */
    @POST("api/v1/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): Response<GenericApiResponse<CreateConversationResponse>>

    /**
     * 更新会话的用户级别设置，如置顶、静音等
     * @param id 会话ID
     * @param request 更新会话设置的请求参数
     */
    @PUT("api/v1/conversations/{id}")
    suspend fun updateConversationSettings(
        @Path("id") id: String,
        @Body request: UpdateConversationSettingsRequest
    ): Response<GenericApiResponse<ConversationUserSettings>>

    /**
     * 从当前用户的会话列表中删除指定会话
     * @param id 会话ID
     */
    @DELETE("api/v1/conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String): Response<GenericApiResponse<Unit>>

    /**
     * 将指定会话标记为已读状态
     * @param id 会话ID
     * @param request 标记会话为已读的请求参数
     */
    @PUT("api/v1/conversations/{id}/read")
    suspend fun markConversationAsRead(
        @Path("id") id: String,
        @Body request: MarkConversationReadRequest
    ): Response<GenericApiResponse<Unit>>
}
