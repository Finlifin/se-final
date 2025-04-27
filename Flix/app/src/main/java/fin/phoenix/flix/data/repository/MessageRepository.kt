package fin.phoenix.flix.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import fin.phoenix.flix.api.ConnectionState
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.*
import fin.phoenix.flix.data.room.AppDatabase
import fin.phoenix.flix.data.room.ConversationEntity
import fin.phoenix.flix.data.room.MessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Date

class MessageRepository(
    private val context: Context,
    private val messageClient: PhoenixMessageClient = PhoenixMessageClient.instance
) {
    private val messageDao = AppDatabase.getDatabase(context).messageDao()
    private val conversationDao = AppDatabase.getDatabase(context).conversationDao()
    
    // 新消息流，用于通知UI层
    private val _newMessages = MutableSharedFlow<Message>()
    val newMessages: SharedFlow<Message> = _newMessages
    
    // 直接传递PhoenixMessageClient的连接状态
    val connectionState: LiveData<ConnectionState> = messageClient.connectionState
    
    // 直接传递PhoenixMessageClient的未读消息计数
    val unreadCounts: LiveData<UnreadCounts> = messageClient.unreadCounts
    
    init {
        // 监听Phoenix客户端的新消息
        messageClient.newMessages
            .onEach { message -> handleNewMessage(message) }
            .catch { e -> Log.e(TAG, "处理新消息时出错", e) }
            .launchIn(CoroutineScope(Dispatchers.IO + SupervisorJob()))
            
        // 监听消息状态变化
        messageClient.messageStatusChanges
            .onEach { statusChange -> handleMessageStatusChange(statusChange) }
            .catch { e -> Log.e(TAG, "处理消息状态变化时出错", e) }
            .launchIn(CoroutineScope(Dispatchers.IO + SupervisorJob()))
    }

    private suspend fun handleNewMessage(message: Message) {
        if (message.senderId == messageClient.userId) {
            return // 忽略自己发送的消息
        }

        // 1. 保存消息到本地数据库
        saveMessage(message)

        // 2. 更新会话信息
        updateConversationForMessage(message)

        // 3. 通知 UI 层
        _newMessages.emit(message)
    }
    
    private suspend fun handleMessageStatusChange(statusChange: fin.phoenix.flix.api.MessageStatusChange) {
        // 更新本地数据库中的消息状态
        messageDao.updateMessageStatus(statusChange.messageId, statusChange.newStatus)
        
        // 如果状态变为已读，更新对应会话的未读计数
        if (statusChange.newStatus == MessageStatus.READ) {
            val message = messageDao.getMessageById(statusChange.messageId)
            message?.let {
                val conversation = conversationDao.getConversationByConversationId(it.conversationId)
                conversation?.let { conv ->
                    // 减少未读计数
                    if (conv.unreadCount > 0) {
                        conversationDao.updateUnreadCount(conv.id, conv.unreadCount - 1)
                    }
                    
                    // 更新最后已读消息ID
                    conversationDao.updateLastReadMessage(conv.id, statusChange.messageId)
                }
            }
        }
    }

    private suspend fun updateConversationForMessage(message: Message) {
        val conversation = getConversationById(message.conversationId)
        
        if (conversation == null) {
            // 从服务器获取并保存会话信息
            messageClient.getConversation(message.conversationId)
                .onSuccess { createOrUpdateConversation(it) }
        } else {
            // 更新现有会话
            val updatedConversation = conversation.copy(
                conversation = conversation.conversation.copy(
                    lastMessageContent = message.content.firstOrNull()?.payload?.toString(),
                    lastMessageTimestamp = message.insertedAt,
                    lastMessageId = message.id
                ),
                userSettings = conversation.userSettings.copy(
                    unreadCount = conversation.userSettings.unreadCount + 1
                )
            )
            createOrUpdateConversation(updatedConversation)
        }
    }

    suspend fun sync() = withContext(Dispatchers.IO) {
        try {
            // 同步消息
            messageClient.syncMessages()
                .onSuccess { result ->
                    result.events.forEach { event ->
                        if (event.eventType == "new_message") {
                            val message = event.payload as Message
                            saveMessage(message)
                            _newMessages.emit(message)
                        } else if (event.eventType == "message_status_changed") {
                            val message = event.payload as Message
                            messageDao.updateMessageStatus(message.id, message.status)
                        } else if (event.eventType == "message_recalled") {
                            val conversation = event.payload as ConversationDetail
                            createOrUpdateConversation(conversation)
                        }
                    }
                }

            // 同步会话列表
            messageClient.getConversations()
                .onSuccess { conversations ->
                    conversations.forEach { createOrUpdateConversation(it) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "同步失败", e)
        }
    }

    suspend fun markMessageAsRead(messageId: String) = withContext(Dispatchers.IO) {
        // 更新本地数据库
        messageDao.updateMessageStatus(messageId, MessageStatus.READ)
        
        // 同步到服务器
        val message = messageDao.getMessageById(messageId)
        message?.let {
            messageClient.markMessagesRead(it.conversationId, messageId)
            
            // 更新会话的未读计数
            val conversation = conversationDao.getConversationByConversationId(it.conversationId)
            conversation?.let { conv ->
                if (conv.unreadCount > 0) {
                    conversationDao.updateUnreadCount(conv.id, conv.unreadCount - 1)
                }
                conversationDao.updateLastReadMessage(conv.id, messageId)
            }
        }
    }
    
    /**
     * 将会话中的所有未读消息标记为已读
     */
    suspend fun markConversationAsRead(conversationId: String) = withContext(Dispatchers.IO) {
        try {
            // 获取会话中最新的消息ID
            val latestMessage = messageDao.getMessagesForConversation(conversationId, 1)
                .first().firstOrNull()
                
            latestMessage?.let { message ->
                // 更新所有未读消息状态
                messageDao.updateAllUnreadMessagesInConversation(conversationId, MessageStatus.READ)
                
                // 同步到服务器
                messageClient.markMessagesRead(conversationId, message.id)
                
                // 更新会话的未读计数
                conversationDao.updateUnreadCount(conversationId, 0)
                conversationDao.updateLastReadMessage(conversationId, message.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "将会话标记为已读失败", e)
        }
    }

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
    
    suspend fun clearConversation(conversationId: String) {
        conversationDao.getConversationByConversationId(conversationId)?.let {
            val conversation = it.copy(
                lastMessageId = null,
                lastMessageContent = null,
                lastMessageTimestamp = null,
                unreadCount = 0,
                lastReadMessageId = null
            )
            conversationDao.updateConversation(conversation)
            messageDao.clearConversation(conversationId)
        }
    }
    
    // 消息相关操作
    fun getMessagesForConversation(conversationId: String, limit: Int = 20): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId, limit).map { entities ->
            // 获取消息后自动标记为已读
            val messages = entities.map { it.toMessage() }
            // 自动标记消息为已读
            markIncomingMessagesAsRead(messages, conversationId)
            messages
        }
    }
    
    private suspend fun markIncomingMessagesAsRead(messages: List<Message>, conversationId: String) {
        // 获取当前用户ID
        val userId = messageClient.userId
        
        // 筛选出未读且不是自己发送的消息
        val unreadMessages = messages.filter { 
            it.status == MessageStatus.UNREAD && it.senderId != userId 
        }
        
        if (unreadMessages.isNotEmpty()) {
            // 找出最新的一条消息ID
            val latestMessageId = unreadMessages.maxByOrNull { it.insertedAt }?.id
            
            latestMessageId?.let {
                // 标记为已读
                markMessageAsRead(it)
            }
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
        saveMessage(sendingMessage)

        // 通过WebSocket发送
        messageClient.sendMessage(
            conversationId = message.conversationId,
            content = message.content,
            messageType = message.messageType
        ).onSuccess { serverMessage ->
            // 更新本地消息状态
            messageDao.updateMessageStatus(serverMessage.id, "sent")
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

    /*
    保存消息到本地数据库
     */
    private suspend fun saveMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
        Log.d("MessageDao", "Message saved: $message")
    }
    
    suspend fun retryFailedMessage(message: Message): Result<Message> {
        return sendMessage(message.copy(isSending = false, errorMessage = null))
    }
    
    suspend fun withdrawMessage(messageId: String) {
        messageClient.withdrawMessage(messageId).onSuccess {
            messageDao.updateMessageStatus(messageId, "withdrawn")
        }
    }
    
    /**
     * 获取某个会话的未读消息数量
     */
    suspend fun getUnreadCountForConversation(conversationId: String): Flow<Int> {
        return conversationDao.getConversationByConversationId(conversationId)?.let { conv ->
            flowOf(conv.unreadCount)
        } ?: flowOf(0)
    }
    
    /**
     * 获取用户的所有未读消息数量
     */
    suspend fun getAllUnreadMessageCounts(): UnreadCounts {
        return messageClient.unreadCounts.value ?: UnreadCounts()
    }
    
    /**
     * 连接到消息服务器
     */
//    suspend fun connect() {
//        messageClient.connect()
//    }
    
    /**
     * 断开消息服务器连接
     */
    fun disconnect() {
        messageClient.disconnect()
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

    companion object {
        private const val TAG = "MessageRepository"
        @Volatile
        private var INSTANCE: MessageRepository? = null
        
        fun getInstance(context: Context): MessageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessageRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}