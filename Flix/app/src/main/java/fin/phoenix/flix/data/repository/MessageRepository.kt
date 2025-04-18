package fin.phoenix.flix.data.repository

import android.content.Context
import androidx.lifecycle.asLiveData
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.ConversationDetail
import fin.phoenix.flix.data.ConversationUserSettings
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.room.AppDatabase
import fin.phoenix.flix.data.room.ConversationEntity
import fin.phoenix.flix.data.room.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date

class MessageRepository(
    context: Context, 
    private val messageClient: PhoenixMessageClient = PhoenixMessageClient.instance
) {
    private val messageDao = AppDatabase.getDatabase(context).messageDao()
    private val conversationDao = AppDatabase.getDatabase(context).conversationDao()
    
    // 会话相关操作
    fun getAllConversations(limit: Int = 20): Flow<List<ConversationDetail>> {
        return conversationDao.getAllConversations(limit).map { entities ->
            entities.map { it.toConversationDetail() }
        }
    }
    
    suspend fun createOrUpdateConversation(conversationDetail: ConversationDetail) {
        conversationDao.insertConversation(conversationDetail.toEntity())
    }
    
    suspend fun getConversationById(id: String): ConversationDetail? {
        return conversationDao.getConversationById(id)?.toConversationDetail()
    }
    
    suspend fun updateConversationUnreadCount(id: String, count: Int) {
        conversationDao.updateUnreadCount(id, count)
    }
    
    suspend fun toggleConversationPin(id: String, isPinned: Boolean) {
        conversationDao.updatePinned(id, isPinned)
    }
    
    suspend fun toggleConversationMute(id: String, isMuted: Boolean) {
        conversationDao.updateMuted(id, isMuted)
    }
    
    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversation(id)
        messageDao.deleteMessagesForConversation(id)
    }
    
    // 消息相关操作
    fun getMessagesForConversation(conversationId: String, limit: Int = 20): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId, limit).map { entities ->
            entities.map { it.toMessage() }
        }
    }
    
    fun getMessagesForConversationBefore(conversationId: String, before: Date, limit: Int = 20): Flow<List<Message>> {
        return messageDao.getMessagesForConversationBefore(conversationId, before.time, limit).map { entities ->
            entities.map { it.toMessage() }
        }
    }
    
    suspend fun sendMessage(message: Message): Result<Message> = withContext(Dispatchers.IO) {
        // 先保存到本地数据库
        val sendingMessage = message.copy(isSending = true)
        messageDao.insertMessage(sendingMessage.toEntity())
        
        // 通过WebSocket发送
        messageClient.sendMessage(
            conversationId = message.conversationId,
            content = message.content,
            messageType = message.messageType
        ).onSuccess { serverMessage ->
            // 更新本地消息状态
            messageDao.insertMessage(serverMessage.toEntity())
        }.onFailure { exception ->
            // 更新发送失败状态
            messageDao.updateMessage(
                sendingMessage.copy(
                    isSending = false,
                    errorMessage = exception.message,
                    sendAttempts = sendingMessage.sendAttempts + 1
                ).toEntity()
            )
        }
    }
    
    suspend fun retryFailedMessage(message: Message): Result<Message> {
        return sendMessage(message.copy(isSending = false, errorMessage = null))
    }
    
    suspend fun markMessageAsRead(messageId: String) {
        messageDao.updateMessageStatus(messageId, "read")
    }
    
    suspend fun withdrawMessage(messageId: String) {
        messageClient.withdrawMessage(messageId).onSuccess {
            messageDao.updateMessageStatus(messageId, "withdrawn")
        }
    }

    // 扩展函数用于Entity和Domain对象之间的转换
    private fun MessageEntity.toMessage(): Message = Message(
        id = id,
        clientMessageId = clientMessageId,
        senderId = senderId,
        receiverId = receiverId,
        conversationId = conversationId,
        content = content,
        messageType = messageType,
        status = status,
        referenceId = referenceId,
        clientTimestamp = clientTimestamp,
        serverTimestamp = serverTimestamp,
        insertedAt = insertedAt,
        updatedAt = updatedAt,
        isSending = isSending,
        sendAttempts = sendAttempts,
        errorMessage = errorMessage
    )

    private fun Message.toEntity(): MessageEntity = MessageEntity(
        id = id,
        clientMessageId = clientMessageId,
        senderId = senderId,
        receiverId = receiverId,
        conversationId = conversationId,
        content = content,
        messageType = messageType,
        status = status,
        referenceId = referenceId,
        clientTimestamp = clientTimestamp,
        serverTimestamp = serverTimestamp,
        insertedAt = insertedAt,
        updatedAt = updatedAt,
        isSending = isSending,
        sendAttempts = sendAttempts,
        errorMessage = errorMessage
    )

    // 更新为新的转换方法，适配新的Conversation类定义
    private fun ConversationEntity.toConversationDetail(): ConversationDetail = ConversationDetail(
        conversation = Conversation(
            id = id,
            conversationId = conversationId,
            type = type,
            participantIds = participantIds,
            lastMessageId = lastMessageId,
            lastMessageContent = lastMessageContent,
            lastMessageTimestamp = lastMessageTimestamp,
            updatedAt = updatedAt,
            insertedAt = insertedAt
        ),
        userSettings = ConversationUserSettings(
            unreadCount = unreadCount,
            isPinned = isPinned,
            isMuted = isMuted,
            lastReadMessageId = lastReadMessageId,
            draft = null // ConversationEntity中没有draft字段，可以根据需要添加
        )
    )

    private fun ConversationDetail.toEntity(): ConversationEntity = ConversationEntity(
        id = conversation.id,
        conversationId = conversation.conversationId,
        type = conversation.type,
        participantIds = conversation.participantIds,
        lastMessageId = conversation.lastMessageId,
        lastMessageContent = conversation.lastMessageContent,
        lastMessageTimestamp = conversation.lastMessageTimestamp,
        updatedAt = conversation.updatedAt,
        insertedAt = conversation.insertedAt,
        unreadCount = userSettings.unreadCount,
        isPinned = userSettings.isPinned,
        isMuted = userSettings.isMuted,
        lastReadMessageId = userSettings.lastReadMessageId
    )
}