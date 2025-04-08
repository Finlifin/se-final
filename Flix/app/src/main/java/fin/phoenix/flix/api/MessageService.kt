package fin.phoenix.flix.api

import fin.phoenix.flix.repository.AnnouncementListResponse
import fin.phoenix.flix.repository.AnnouncementResponse
import fin.phoenix.flix.repository.CreateMessageRequest
import fin.phoenix.flix.repository.MarkMessagesReadRequest
import fin.phoenix.flix.repository.MarkReadResponse
import fin.phoenix.flix.repository.MessageListResponse
import fin.phoenix.flix.repository.MessageResponse
import fin.phoenix.flix.repository.SyncRequest
import fin.phoenix.flix.repository.SyncResponse
import fin.phoenix.flix.repository.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query


/**
 * 消息API服务接口
 */
interface MessageService {
    @GET("messages")
    suspend fun getMessages(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("message_type") messageType: String? = null,
        @Query("status") status: String? = null
    ): Response<MessageListResponse>

    @GET("messages/{id}")
    suspend fun getMessage(@Path("id") id: String): Response<MessageResponse>

    @POST("messages")
    suspend fun createMessage(@Body request: CreateMessageRequest): Response<MessageResponse>

    @PUT("messages/{id}/read")
    suspend fun markMessageAsRead(@Path("id") id: String): Response<MessageResponse>

    @PUT("messages/batch_read")
    suspend fun markMessagesAsRead(@Body request: MarkMessagesReadRequest): Response<MarkReadResponse>

    @PUT("messages/read_all")
    suspend fun markAllMessagesAsRead(): Response<MarkReadResponse>

    @GET("messages/unread_count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @DELETE("messages/{id}")
    suspend fun deleteMessage(@Path("id") id: String): Response<Unit>

    @POST("messages/sync")
    suspend fun syncMessages(@Body request: SyncRequest): Response<SyncResponse>

    @GET("announcements")
    suspend fun getAnnouncements(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<AnnouncementListResponse>

    @GET("announcements/{id}")
    suspend fun getAnnouncement(@Path("id") id: String): Response<AnnouncementResponse>
}
