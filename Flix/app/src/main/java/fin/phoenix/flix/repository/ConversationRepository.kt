package fin.phoenix.flix.repository

import android.content.Context
import fin.phoenix.flix.api.ConversationService
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.data.*
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 会话仓库类，处理会话相关操作
 */
class ConversationRepository(private val context: Context) {
    private val conversationService = RetrofitClient.createService(ConversationService::class.java, context)

    /**
     * 获取所有会话列表
     */
    suspend fun getConversations(): Resource<List<ConversationListItem>> =
        withContext(Dispatchers.IO) {
            conversationService.getConversations().toResource("获取会话列表失败")
        }

    /**
     * 获取会话详情
     * @param id 会话ID
     */
    suspend fun getConversation(id: String): Resource<ConversationDetail> =
        withContext(Dispatchers.IO) {
            conversationService.getConversation(id).toResource("获取会话详情失败")
        }

    /**
     * 创建新会话
     * @param type 会话类型：private或group
     * @param participantIds 参与者ID列表
     */
    suspend fun createConversation(
        type: String,
        participantIds: List<String>
    ): Resource<CreateConversationResponse> =
        withContext(Dispatchers.IO) {
            val request = CreateConversationRequest(type, participantIds)
            conversationService.createConversation(request).toResource("创建会话失败")
        }

    /**
     * 更新会话设置
     * @param id 会话ID
     * @param isPinned 是否置顶
     * @param isMuted 是否静音
     * @param draft 消息草稿
     */
    suspend fun updateConversationSettings(
        id: String,
        isPinned: Boolean? = null,
        isMuted: Boolean? = null,
        draft: String? = null
    ): Resource<ConversationUserSettings> =
        withContext(Dispatchers.IO) {
            val request = UpdateConversationSettingsRequest(isPinned, isMuted, draft)
            conversationService.updateConversationSettings(id, request).toResource("更新会话设置失败")
        }

    /**
     * 删除会话
     * @param id 会话ID
     */
    suspend fun deleteConversation(id: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            conversationService.deleteConversation(id).toResource("删除会话失败")
        }

    /**
     * 标记会话为已读
     * @param id 会话ID
     * @param lastReadMessageId 最后一条已读消息ID
     */
    suspend fun markConversationAsRead(id: String, lastReadMessageId: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val request = MarkConversationReadRequest(lastReadMessageId)
            conversationService.markConversationAsRead(id, request).toResource("标记会话为已读失败")
        }
}
