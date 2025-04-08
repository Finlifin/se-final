package fin.phoenix.flix.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import fin.phoenix.flix.api.MessageService
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.data.ContentTypes
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessagePreview
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.TextMessageContent
import fin.phoenix.flix.data.UnreadCounts
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.GsonConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// API Response classes
data class MessageListResponse(
    val data: List<Message>
)

data class MessageResponse(
    val data: Message
)

data class UnreadCountResponse(
    val data: UnreadCounts
)

data class MarkReadResponse(
    val data: MarkReadData
)

data class MarkReadData(
    val markedCount: Int
)

data class AnnouncementListResponse(
    val data: List<Announcement>
)

data class AnnouncementResponse(
    val data: Announcement
)

data class Announcement(
    val id: String, val title: String, val content: String, val createdAt: Date
)

data class SyncResponse(
    val data: SyncData
)

data class SyncData(
    val messages: List<Message>, val syncTime: String
)

// Request classes
data class CreateMessageRequest(
    val receiverId: String,
    val content: Map<String, Any>,
    val contentType: String,
    val messageType: String,
    val referenceId: String? = null
)

data class MarkMessagesReadRequest(
    val messageIds: List<String>
)

data class SyncRequest(
    val lastSyncTime: String, val userId: String
)

/**
 * 消息仓库，处理消息相关操作
 */
class MessageRepository(private val context: Context, private val phoenixClient: PhoenixMessageClient) {
    private val TAG = "MessageRepository"

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
    }

    private val messageService: MessageService by lazy {
        RetrofitClient.createService(MessageService::class.java, context)
    }

    // WebSocket channel name
    private val channelName: String
        get() {
            val userId = sharedPrefs.getString("user_id", null) ?: ""
            return "user:$userId"
        }

    // Current user ID
    private val currentUserId: String
        get() = sharedPrefs.getString("user_id", "") ?: ""

    // Authentication token
    private val authToken: String?
        get() = sharedPrefs.getString("auth_token", null)

    // 会话列表状态
    private val _conversations = MutableStateFlow<List<MessagePreview>>(emptyList())
    val conversations: StateFlow<List<MessagePreview>> = _conversations

    // 未读消息计数
    private val _unreadCounts = MutableStateFlow(UnreadCounts())
    val unreadCounts: StateFlow<UnreadCounts> = _unreadCounts

    // 新消息Flow
    private val _newMessages = MutableStateFlow<Message?>(null)
    val newMessages: StateFlow<Message?> = _newMessages

    /**
     * 获取未读消息计数
     */
    suspend fun fetchUnreadCounts(): Resource<UnreadCounts> = withContext(Dispatchers.IO) {
        try {
            // 尝试使用WebSocket获取未读计数
            try {
                val response = phoenixClient.push(
                    channelName, "get_unread_count", JSONObject()
                )

                val countsJson = response.optJSONObject("counts")
                if (countsJson != null) {
                    val unreadCounts = UnreadCounts(
                        total = countsJson.optInt("total", 0),
                        systemNotification = countsJson.optInt("system_notification", 0),
                        systemAnnouncement = countsJson.optInt("system_announcement", 0),
                        interaction = countsJson.optInt("interaction", 0),
                        privateMessage = countsJson.optInt("private_message", 0)
                    )
                    _unreadCounts.value = unreadCounts
                    return@withContext Resource.Success(unreadCounts)
                }
            } catch (e: Exception) {
                Log.e(TAG, "通过WebSocket获取未读计数失败", e)
                // 失败后尝试使用REST API
            }

            // 回退到使用REST API
            val response = messageService.getUnreadCount()
            if (response.isSuccessful) {
                val unreadCounts = response.body()?.data ?: UnreadCounts()
                _unreadCounts.value = unreadCounts
                return@withContext Resource.Success(unreadCounts)
            } else {
                return@withContext Resource.Error("获取未读消息计数失败：${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取未读消息计数失败", e)
            return@withContext Resource.Error("获取未读消息计数失败：${e.message}")
        }
    }

    /**
     * 同步离线消息
     */
    private suspend fun syncOfflineMessages() {
        try {
            val lastSyncTime = getLastSyncTime()
            val syncRequest = SyncRequest(lastSyncTime, currentUserId)

            when (val result = syncMessages(syncRequest)) {
                is Resource.Success -> {
                    // 处理新消息
                    result.data.data.messages.forEach { message ->
                        _newMessages.emit(message)
                    }
                    // 更新同步时间
                    result.data.data.syncTime.let { syncTime ->
                        saveLastSyncTime(syncTime)
                    }
                }

                is Resource.Error -> {
                    Log.e(TAG, "同步离线消息失败: ${result.message}")
                }

                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步离线消息失败", e)
        }
    }

    /**
     * 获取上次同步时间
     */
    private fun getLastSyncTime(): String {
        return sharedPrefs.getString("last_sync_time", null) ?: ISO8601_DATE_FORMAT.format(Date(0))
    }

    /**
     * 保存同步时间
     */
    private fun saveLastSyncTime(syncTime: String) {
        sharedPrefs.edit().putString("last_sync_time", syncTime).apply()
    }

    /**
     * 初始化WebSocket连接
     */
    suspend fun initWebSocket(channelName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("No auth token"))

            if (phoenixClient.isConnected) return@withContext Result.success(true)

            Log.d(TAG, "正在初始化WebSocket连接...")

            // Connect with retry
            var attempt = 0
            var lastError: Exception? = null

            while (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    if (phoenixClient.connect(token)) {
                        // Join channel after successful connection
                        try {
                            val response = phoenixClient.joinChannel(channelName, token)
                            Log.d(TAG, "成功加入频道：$channelName")

                            // 初始化数据
                            fetchInitialData()

                            return@withContext Result.success(true)
                        } catch (e: Exception) {
                            Log.e(TAG, "加入频道失败", e)
                            lastError = e
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "连接尝试 $attempt 失败", e)
                    lastError = e
                }

                attempt++
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    delay(RETRY_DELAY_MS * attempt) // 指数退避
                }
            }

            return@withContext Result.failure(
                lastError
                    ?: Exception("Failed to initialize WebSocket after $MAX_RETRY_ATTEMPTS attempts")
            )
        } catch (e: Exception) {
            Log.e(TAG, "初始化WebSocket失败", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * 初始化数据
     */
    private suspend fun fetchInitialData() {
        try {
            // 1. 获取未读消息计数
            fetchUnreadCounts()

            // 2. 同步离线消息
            val lastSyncTime = getLastSyncTime()
            syncOfflineMessages()

            // 3. 更新最后同步时间
            saveLastSyncTime(ISO8601_DATE_FORMAT.format(Date()))

            // 4. 加载最近会话
            loadRecentConversations()
        } catch (e: Exception) {
            Log.e(TAG, "获取初始数据失败", e)
        }
    }

    /**
     * 带重试机制的安全网络请求
     */
    private suspend fun <T> safeApiCall(
        call: suspend () -> Response<T>, errorMessage: String
    ): Resource<T> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                Resource.Success(response.body()!!)
            } else {
                Log.e(TAG, "$errorMessage: ${response.code()}")
                Resource.Error("$errorMessage: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, errorMessage, e)
            Resource.Error("$errorMessage: ${e.message}")
        }
    }

    /**
     * 带重试机制的WebSocket操作
     */
    private suspend fun <T> safeWebSocketOperation(
        operation: suspend () -> T,
        fallback: (suspend () -> Resource<T>)? = null,
        errorMessage: String
    ): Resource<T> = withContext(Dispatchers.IO) {
        try {
            // 尝试WebSocket操作
            val result = operation()
            Resource.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket $errorMessage", e)

            // 如果提供了回退操作，则尝试执行
            if (fallback != null) {
                try {
                    fallback()
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "Fallback operation failed", fallbackError)
                    Resource.Error("$errorMessage: ${fallbackError.message}")
                }
            } else {
                Resource.Error("$errorMessage: ${e.message}")
            }
        }
    }

    /**
     * 加载会话列表，带错误处理和回退机制
     */
suspend fun loadRecentConversations(messageType: String? = null): Resource<List<MessagePreview>> {
    return withContext(Dispatchers.IO) {
        try {
            safeApiCall(
                call = {
                    messageService.getMessages(limit = 20, messageType = messageType)
                },
                errorMessage = "Failed to get conversations"
            ).let { result ->
                when (result) {
                    is Resource.Success -> {
                        val messages = completeSender(result.data.data)
                        val conversations = processMessagesToConversations(result.data.data)
                        _conversations.value = conversations
                        Resource.Success(conversations)
                    }
                    is Resource.Error -> Resource.Error(result.message)
                    is Resource.Loading -> Resource.Loading
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load conversations", e)
            Resource.Error("加载会话列表失败：${e.message}")
        }
    }
}

    // get sender info from message.senderId
    private suspend fun completeSender(messages: List<Message>): List<Message> {
        val userRepository = ProfileRepository(context)
        return messages.map { message ->
            val senderId = message.senderId
            if (senderId != null) {
                when (val result = userRepository.getUserById(message.senderId)) {
                    is Resource.Success -> {
                        message.sender = result.data
                        message
                    }

                    is Resource.Error -> {
                        Log.e(TAG, "获取用户信息失败: ${result.message}")
                        message
                    }

                    is Resource.Loading -> {
                        message
                    }
                }
            } else {
                message
            }
        }
    }

    /**
     * 将JSON数组解析为会话列表
     */
    private fun parseConversationsFromJson(jsonArray: JSONArray): List<MessagePreview> {
        val conversations = mutableListOf<MessagePreview>()
        for (i in 0 until jsonArray.length()) {
            try {
                val conversationJson = jsonArray.getJSONObject(i)
                val gson = GsonConfig.createGson()
                val conversation =
                    gson.fromJson(conversationJson.toString(), MessagePreview::class.java)
                conversations.add(conversation)
            } catch (e: Exception) {
                Log.e(TAG, "解析会话失败", e)
            }
        }
        return conversations
    }

    /**
     * 将消息列表处理为会话列表
     */
    private fun processMessagesToConversations(messages: List<Message>): List<MessagePreview> {
        // 按发送者或接收者分组消息
        val conversationMap = mutableMapOf<String, MutableList<Message>>()

        messages.forEach { message ->
            val partnerId =
                if (message.senderId == currentUserId) message.receiverId else message.senderId
            if (!conversationMap.containsKey(partnerId)) {
                conversationMap[partnerId!!] = mutableListOf()
            }
            conversationMap[partnerId]?.add(message)
        }

        // 为每个会话提取最新消息
        return conversationMap.map { (partnerId, messageList) ->
            // 按时间排序并获取最新消息
            val latestMessage = messageList.maxByOrNull { it.insertedAt } ?: return@map null

            // 创建会话预览
            Log.d(TAG, "最新消息: $latestMessage")
            val partner =
                (latestMessage.sender?.userName.takeIf { partnerId == latestMessage.senderId }
                    ?: latestMessage.receiver?.userName)?.let {
                    UserAbstract(
                        uid = partnerId,
                        userName = it,
                        avatarUrl = latestMessage.sender?.avatarUrl  // 需要从其他地方获取头像
                    )
                }

            // 提取消息内容为预览文本
            val previewText = when (latestMessage.contentType) {
                ContentTypes.TEXT -> {
                    val textContent = latestMessage.content as? TextMessageContent
                    textContent?.text ?: "新消息"
                }

                else -> "新消息"
            }

            MessagePreview(
                conversationId = partnerId,
                lastMessage = latestMessage.content.toString(),
                user = partner!!,
                timestamp = latestMessage.insertedAt,
                messageType = latestMessage.messageType,
                unreadCount = messageList.count {
                    it.senderId != currentUserId
                },
                id = "con-${latestMessage.id}",
                lastSenderId = latestMessage.senderId!!
            )
        }.filterNotNull()
    }


/**
 * 加载聊天历史，使用REST API
 */
suspend fun loadChatHistory(partnerId: String): Resource<List<Message>> {
    return withContext(Dispatchers.IO) {
        try {
            safeApiCall(
                errorMessage = "Failed to get chat history",
                call = {
                    messageService.getMessages(
                        limit = 50,
                        messageType = MessageTypes.PRIVATE_MESSAGE
                    )
                }
            ).map { messages ->
                messages.data
                    .filter {
                        (it.senderId == partnerId && it.receiverId == currentUserId) ||
                        (it.senderId == currentUserId && it.receiverId == partnerId)
                    }
                    .sortedBy { it.insertedAt }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load chat history", e)
            Resource.Error("加载聊天历史失败：${e.message}")
        }
    }
}

    /**
     * 标记消息已读，带错误处理和回退机制
     */
    suspend fun markMessageAsRead(messageId: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 优先使用WebSocket标记已读
                val webSocketResult = safeWebSocketOperation(
                    operation = {
                    phoenixClient.ackMessage(channelName, messageId)
                    true
                }, fallback = {
                    // 失败时回退到REST API
                    safeApiCall(
                        call = { messageService.markMessageAsRead(messageId) },
                        errorMessage = "Failed to mark message as read through REST API"
                    ).map { true }
                }, errorMessage = "Failed to mark message as read through WebSocket"
                )

                webSocketResult
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark message as read", e)
                Resource.Error("标记消息已读失败：${e.message}")
            }
        }
    }

    /**
     * 批量标记消息已读，带错误处理和回退机制
     */
    suspend fun markMessagesAsRead(messageIds: List<String>): Resource<Boolean> {
        if (messageIds.isEmpty()) return Resource.Success(true)

        return withContext(Dispatchers.IO) {
            try {
                // 优先使用WebSocket批量标记已读
                val webSocketResult = safeWebSocketOperation(
                    operation = {
                    phoenixClient.ackMessages(channelName, messageIds)
                    true
                }, fallback = {
                    // 失败时回退到REST API
                    safeApiCall(
                        call = {
                            messageService.markMessagesAsRead(
                                MarkMessagesReadRequest(messageIds)
                            )
                        }, errorMessage = "Failed to mark messages as read through REST API"
                    ).map { true }
                }, errorMessage = "Failed to mark messages as read through WebSocket"
                )

                webSocketResult
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as read", e)
                Resource.Error("批量标记消息已读失败：${e.message}")
            }
        }
    }

    /**
     * 同步离线消息，带错误处理和回退机制
     */
    suspend fun syncMessages(request: SyncRequest): Resource<SyncResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // 优先使用WebSocket同步
                val webSocketResult = safeWebSocketOperation(
                    operation = {
                    val response = phoenixClient.syncMessages(channelName, request.lastSyncTime)
                    // 解析同步响应
                    val messages = response.optJSONArray("messages")?.let { parseMessages(it) }
                    val syncTime = response.optString("sync_time")
                    SyncResponse(SyncData(messages ?: emptyList(), syncTime))
                }, fallback = {
                    // 失败时回退到REST API
                    safeApiCall(
                        call = { messageService.syncMessages(request) },
                        errorMessage = "Failed to sync messages through REST API"
                    )
                }, errorMessage = "Failed to sync messages through WebSocket"
                )

                webSocketResult
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync messages", e)
                Resource.Error("同步消息失败：${e.message}")
            }
        }
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        receiverId: String,
        text: String,
        itemId: String? = null,
        title: String? = null,
        deepLink: String? = null
    ): Resource<Message> = withContext(Dispatchers.IO) {
        try {
            // 优先使用WebSocket发送消息
            val webSocketResult = safeWebSocketOperation(
                operation = {
                val response = phoenixClient.sendPrivateMessage(
                    channelName = channelName,
                    receiverId = receiverId,
                    text = text,
                    itemId = itemId,
                    title = title,
                    deepLink = deepLink
                )

                val messageJson = response.optJSONObject("message")
                    ?: throw Exception("Invalid response format")

                val gson = GsonConfig.createGson()
                gson.fromJson(messageJson.toString(), Message::class.java)
            }, fallback = {
                // 失败时回退到REST API
                val content = mutableMapOf<String, Any>("text" to text)
                if (itemId != null) content["item_id"] = itemId
                if (title != null) content["title"] = title
                if (deepLink != null) content["deep_link"] = deepLink

                val request = CreateMessageRequest(
                    receiverId = receiverId,
                    content = content,
                    contentType = "text",
                    messageType = MessageTypes.PRIVATE_MESSAGE
                )

                safeApiCall(
                    call = { messageService.createMessage(request) },
                    errorMessage = "Failed to send message through REST API"
                ).map { it.data }
            }, errorMessage = "Failed to send message through WebSocket"
            )

            webSocketResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Resource.Error("发送消息失败：${e.message}")
        }
    }

    companion object {
        private val ISO8601_DATE_FORMAT =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L // 1秒基础延迟
    }
}

// extend map function for Resource
fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> {
    return when (this) {
        is Resource.Success -> Resource.Success(transform(data))
        is Resource.Error -> Resource.Error(message)
        is Resource.Loading -> Resource.Loading
    }
}


fun parseMessages(jsonArray: JSONArray): List<Message> {
    val gson = GsonConfig.createPrettyGson()
    return List(jsonArray.length()) { index ->
        val messageJson = jsonArray.getJSONObject(index)
        gson.fromJson(messageJson.toString(), Message::class.java)
    }
}