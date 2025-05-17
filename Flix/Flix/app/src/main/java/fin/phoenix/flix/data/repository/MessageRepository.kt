package fin.phoenix.flix.data.repository

import android.content.Context
import android.nfc.Tag
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import fin.phoenix.flix.api.ConnectionState
import fin.phoenix.flix.api.PhoenixError
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.MessageStatus
import fin.phoenix.flix.data.SyncResult
import fin.phoenix.flix.data.UnreadCounts
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.data.room.AppDatabase
import fin.phoenix.flix.data.room.ConversationDao
import fin.phoenix.flix.data.room.ConversationEntity
import fin.phoenix.flix.data.room.MessageDao
import fin.phoenix.flix.data.room.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 消息仓库类
 * 集成了远程消息客户端和本地数据库，为应用提供统一的消息数据访问层
 */
class MessageRepository private constructor(
    private val context: Context, private val userManager: UserManager
) {
    private val TAG = "MessageRepository"

    // Phoenix WebSocket 客户端
    val messageClient = PhoenixMessageClient.instance

    // 数据库相关
    private var appDatabase: AppDatabase? = null
    private var messageDao: MessageDao? = null
    private var conversationDao: ConversationDao? = null

    // 协程作用域
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 当前用户ID
    private var currentUserId: String? = null

    // 连接状态
    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    val connectionState = messageClient.connectionState

    private val currentUser = UserManager.getInstance(context)

    // 错误状态
    private val _errors = MutableStateFlow<PhoenixError?>(null)
    val errors: StateFlow<PhoenixError?> = _errors.asStateFlow()

    // 初始化
    init {
        // 观察用户状态变化
        userManager.currentUserId.observeForever { userId ->
            repositoryScope.launch {
                handleUserChange(userId)
            }
        }

        // 观察WebSocket连接状态
        messageClient.connectionState.observeForever { state ->
            _connectionState.value = state
            Log.d(TAG, "在MessageRepository中观察到连接状态变化: $state")
            repositoryScope.launch {
                if (getAuthToken() != null && currentUserId != null) connectToMessageServer(
                    getAuthToken()!!, currentUserId!!
                )
            }
        }

        // 观察新消息
        repositoryScope.launch {
            messageClient.newMessages.collect { message ->
                handleNewMessage(message)
            }
        }

        // 观察错误
        repositoryScope.launch {
            messageClient.errors.collect { error ->
                _errors.value = error
            }
        }
    }

    /**
     * 处理用户变更
     */
    private suspend fun handleUserChange(userId: String?) = withContext(Dispatchers.IO) {
        // 如果用户退出，断开连接和关闭数据库
        if (userId == null || userId.isEmpty()) {
            messageClient.disconnect()
            closeDatabase()
            currentUserId = null
            return@withContext
        }

        // 如果是同一个用户，不做任何处理
        if (userId == currentUserId) {
            return@withContext
        }

        // 切换用户，关闭之前的数据库连接
        closeDatabase()

        // 保存新的用户ID
        currentUserId = userId

        // 为新用户打开数据库
        openDatabaseForUser(userId)

        // 检查用户是否有认证令牌，如果有则连接WebSocket
        val authToken = getAuthToken()
        if (!authToken.isNullOrEmpty()) {
            connectToMessageServer(authToken, userId)
        }
    }

    /**
     * 打开用户特定的数据库
     */
    private fun openDatabaseForUser(userId: String) {
        appDatabase = AppDatabase.Companion.getInstance(context, userId)
        messageDao = appDatabase?.messageDao()
        conversationDao = appDatabase?.conversationDao()

        Log.d(TAG, "已为用户 $userId 打开数据库")
    }

    /**
     * 关闭数据库连接
     */
    private fun closeDatabase() {
        appDatabase = null
        messageDao = null
        conversationDao = null
    }

    /**
     * 连接到消息服务器
     */
    fun connectToMessageServer(authToken: String, userId: String) {
        if (_connectionState.value == ConnectionState.DISCONNECTED || _connectionState.value == ConnectionState.CONNECTION_ERROR) {
            messageClient.connect(authToken, userId)
        }
        if (_connectionState.value == ConnectionState.CONNECTED) {
            // 连接成功后自动加入用户频道
            repositoryScope.launch {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    messageClient.joinUserChannel()
                }
            }
        }
        // 启用自动同步
        if (_connectionState.value == ConnectionState.JOINED) repositoryScope.launch { syncMessages() }
    }

    /**
     * 断开与消息服务器的连接
     */
    fun disconnectFromMessageServer() {
        messageClient.disconnect()
    }

    /**
     * 处理新消息
     */
    private suspend fun handleNewMessage(message: Message) {
        // 保存消息到数据库
        val messageEntity = MessageEntity.Companion.fromDomainModel(message)
        messageDao?.insert(messageEntity)

        // 更新或创建会话
        updateOrCreateConversation(message)
    }

    /**
     * 更新或创建会话
     */
    private suspend fun updateOrCreateConversation(message: Message) {
        // 确定会话ID和参与者ID
        val conversationId: String
        val participantId: String
        val participant: UserAbstract?

        if (message.senderId == currentUserId) {
            // 我发送的消息
            participantId = message.receiverId
            participant = message.receiver
            conversationId = getConversationId(currentUserId!!, participantId)
        } else {
            // 我接收的消息
            participantId = message.senderId
            participant = message.sender
            conversationId = getConversationId(currentUserId!!, participantId)
        }

        // 查询现有会话
        val existingConversation = conversationDao?.getConversationById(conversationId)

        if (existingConversation != null) {
            // 更新现有会话的最后一条消息ID
            conversationDao?.update(existingConversation.apply {
                lastMessageId = message.id
                unreadCounts =
                    if (message.senderId != currentUserId && (message.status.isNullOrEmpty() || message.status == MessageStatus.UNREAD)) {
                        unreadCounts + 1
                    } else {
                        unreadCounts
                    }
            })
        } else {
            // 创建新会话，设置正确的未读数
            val unreadCount =
                if (message.senderId != currentUserId && (message.status.isNullOrEmpty() || message.status == MessageStatus.UNREAD)) 1 else 0

            val conversation = ConversationEntity(
                id = conversationId,
                participantId = participantId,
                lastMessageId = message.id,
                participant = participant,
                unreadCounts = unreadCount
            )
            conversationDao?.insert(conversation)
        }
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        receiverId: String, content: List<MessageContentItem>, messageType: String
    ): Result<Message> {
        // 检查是否已连接
        Log.d(TAG, "当前连接状态: ${_connectionState.value}")
        if (_connectionState.value != ConnectionState.JOINED) {
            return Result.failure(IllegalStateException("未连接到消息服务器"))
        }

        // 通过客户端发送消息
        val result = messageClient.sendMessage(receiverId, content, messageType)

        result.onSuccess { message ->
            // 补充会话ID
            val conversationId = getConversationId(currentUserId!!, receiverId)
            val completeMessage = message.copy(conversationId = conversationId)

            // 保存到数据库
            val messageEntity = MessageEntity.Companion.fromDomainModel(completeMessage)
            messageDao?.insert(messageEntity)

            // 更新或创建会话
            updateOrCreateConversation(completeMessage)
        }

        return result
    }

    /**
     * 标记消息为已读
     */
    suspend fun markMessagesRead(messageIds: List<String>): Result<Int> {
        // 本地数据库更新
        messageIds.forEach { messageId ->
            messageDao?.updateMessageStatus(messageId, MessageStatus.READ)
        }

        // 查询这些消息所属的会话，并更新未读计数
        val messages = messageIds.mapNotNull { messageDao?.getMessageById(it) }
        val conversationIds = messages.map { it.conversationId }.distinct()

        conversationIds.forEach { conversationId ->
            val unreadCount = messageDao?.getUnreadCount(conversationId) ?: 0
            conversationDao?.updateUnreadCounts(conversationId, unreadCount)
        }

        // 发送到服务器
        return messageClient.markMessagesRead(messageIds)
    }

    /**
     * 标记会话中所有消息为已读
     */
    suspend fun markConversationRead(conversationId: String): Result<Int> {
    // 获取会话中所有未读消息的ID
        val messages = messageDao?.getMessagesByConversationIdPaged(conversationId, 1000, 0)
            ?.filter { it.status == null || it.status == MessageStatus.UNREAD }?.map { it.id } ?: listOf()

        // 如果没有未读消息，返回成功结果
        if (messages.isEmpty()) {
            return Result.success(0)
        }

        // 发送到服务器
        val result = messageClient.markMessagesRead(messages)

        if (result.isSuccess) {
            // 本地数据库更新
            messageDao?.markConversationMessagesAsRead(conversationId)
            conversationDao?.clearUnreadCount(conversationId)

            return result
        } else {
            // 处理错误
            Log.e(TAG, "标记会话消息为已读失败: ${result.exceptionOrNull()}")
            return Result.failure(result.exceptionOrNull() ?: Exception("未知错误"))
        }
    }

    /**
     * 标记所有消息为已读
     */
    suspend fun markAllMessagesRead(): Result<Int> {
        // 获取所有会话ID
        val conversations = conversationDao?.getAllConversations()?.value ?: listOf()

        // 为每个会话标记已读
        conversations.forEach { conversation ->
            messageDao?.markConversationMessagesAsRead(conversation.id)
            conversationDao?.clearUnreadCount(conversation.id)
        }

        // 发送到服务器
        return messageClient.markAllMessagesRead()
    }

    /**
     * 同步消息
     */
    suspend fun syncMessages(): Result<SyncResult> {
        val result = messageClient.syncMessages()

        result.onSuccess { syncResult ->
            // 保存同步到的消息到数据库
            syncResult.messages.forEach { message ->
                handleNewMessage(message)
            }
        }

        return result
    }

    /**
     * 获取指定会话的所有消息
     */
    fun getMessagesByConversationId(conversationId: String): LiveData<List<Message>> {
        return messageDao?.getMessagesByConversationId(conversationId)?.map { entities ->
            entities.map { it.toDomainModel() }
        } ?: MutableLiveData(emptyList())
    }

    /**
     * 分页获取指定会话的消息
     */
    suspend fun getMessagesByConversationIdPaged(
        conversationId: String, limit: Int, offset: Int
    ): List<Message> {
        return messageDao?.getMessagesByConversationIdPaged(conversationId, limit, offset)
            ?.map { it.toDomainModel() } ?: emptyList()
    }

    /**
     * 获取所有会话
     */
    fun getAllConversations(): LiveData<List<Conversation>> {
        return conversationDao?.getAllConversations()?.map { entities ->
            entities.map { entity ->
                // Using a temporary null message since we can't call suspend functions here
                val conversation = entity.toDomainModel().copy(lastMessage = null)

                // Load the last message in a coroutine when observed
                if (entity.lastMessageId != null) {
                    repositoryScope.launch {
                        val lastMessage = entity.lastMessageId?.let {
                            messageDao?.getMessageById(it)?.toDomainModel()
                        }
                        // Update the conversation with the loaded message
                        if (lastMessage != null) {
                            // Use appropriate method to update LiveData value if needed
                            // This depends on how your Conversation class is structured
                        }
                    }
                }

                conversation
            }
        } ?: MutableLiveData(emptyList())
    }

    /**
     * 获取指定ID的会话
     */
    suspend fun getConversationById(conversationId: String): Conversation? {
        val entity = conversationDao?.getConversationById(conversationId) ?: return null
        val lastMessage = entity.lastMessageId?.let {
            messageDao?.getMessageById(it)?.toDomainModel()
        }

        return entity.toDomainModel().copy(lastMessage = lastMessage)
    }

    /**
     * 获取与指定用户的会话
     */
    suspend fun getConversationWithUser(participantId: String): Conversation? {
        val userId = currentUserId ?: return null
        val conversationId = getConversationId(userId, participantId)
        return getConversationById(conversationId)
    }


    /**
     * 获取或创建与用户的会话
     */
    suspend fun getOrCreateConversationWithUser(participantId: String): Conversation {
        val userId = currentUserId ?: throw IllegalStateException("未登录")
        val conversationId = getConversationId(userId, participantId)

        // 尝试获取现有会话
        val existingConversation = conversationDao?.getConversationById(conversationId)

        // 如果不存在，创建新会话
        if (existingConversation == null) {
            val newConversation = ConversationEntity(
                id = conversationId,
                participantId = participantId,
                unreadCounts = 0,
                lastMessageId = null,
                participant = null
            )
            conversationDao?.insert(newConversation)
            return newConversation.toDomainModel()
        }

        // 返回现有会话
        val lastMessage = existingConversation.lastMessageId?.let {
            messageDao?.getMessageById(it)?.toDomainModel()
        }

        return existingConversation.toDomainModel().copy(lastMessage = lastMessage)
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return runCatching {
            // 删除会话
            val conversation = conversationDao?.getConversationById(conversationId)
            if (conversation != null) {
                conversationDao?.delete(conversation)
            }

            // 删除会话中的所有消息
            messageDao?.deleteByConversationId(conversationId)
        }
    }

    /**
     * 获取认证令牌（实际项目中从安全存储中获取）
     */
    private fun getAuthToken(): String? {
        return context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
            .getString("auth_token", null)
    }

    suspend fun getMessageById(messageId: String): Message? {
        return messageDao?.getMessageById(messageId)?.toDomainModel()
    }

    suspend fun sync() {
        val lastSyncTime = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
            .getString("last_sync_time", null)

        val result = messageClient.syncMessages(lastSyncTime)
        if (result.isSuccess) {
            val syncResult = result.getOrNull()
            syncResult?.messages?.forEach { message ->
                // 处理每条消息
                handleNewMessage(message)
            }
            // 更新最后同步时间
            context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE).edit {
                putString("last_sync_time", syncResult?.newLastSyncTimestamp.toString())
            }
        } else {
            Log.e(TAG, "同步失败: ${result.exceptionOrNull()}")
        }

    }

    /**
     * 清空会话中的所有消息
     */
    suspend fun clearConversation(conversationId: String): Result<Unit> = runCatching {
        // 在本地数据库中删除指定会话的所有消息
        messageDao?.deleteByConversationId(conversationId)

        // 更新会话的lastMessageId为null
        val conversation = conversationDao?.getConversationById(conversationId)
        conversation?.let {
            it.lastMessageId = null
            conversationDao?.update(it)
        }

        // 这里可以添加同步到服务器的代码
        // messageClient.clearConversationMessages(conversationId)
    }

    /**
     * 获取未读消息统计
     */
    suspend fun getUnreadStats(): Result<UnreadCounts> {
        return messageClient.getUnreadStats()
    }

    fun establish() {
        if (userManager.currentUserId.value.isNullOrEmpty()) {
            Log.d(TAG, "用户未登录，放弃连接")
            return
        }
        val authToken = getAuthToken()
        if (authToken.isNullOrEmpty()) {
            Log.d(TAG, "认证令牌为空，放弃连接")
            return
        }
        Log.d(TAG, "连接状态: ${_connectionState.value}")
        when (_connectionState.value) {
            ConnectionState.DISCONNECTED, ConnectionState.CONNECTION_ERROR, ConnectionState.CONNECTING -> {
                connectToMessageServer(authToken, userManager.currentUserId.value!!)
            }

            else -> {}
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MessageRepository? = null

        fun getInstance(context: Context): MessageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessageRepository(
                    context.applicationContext, UserManager.Companion.getInstance(context)
                ).also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE?.disconnectFromMessageServer()
            INSTANCE = null
        }
    }
}

/**
 * 生成会话ID (确保两个用户之间的会话ID相同，无论谁发起)
 */
fun getConversationId(userId1: String, userId2: String): String {
    return if (userId1 < userId2) {
        "${userId1}_${userId2}"
    } else {
        "${userId2}_${userId1}"
    }
}