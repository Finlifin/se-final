package fin.phoenix.flix.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.MessageStatus
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.SyncResult
import fin.phoenix.flix.data.UnreadCounts
import fin.phoenix.flix.repository.getConversationId
import fin.phoenix.flix.util.GsonConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Phoenix WebSocket 客户端，用于处理二手交易平台的实时消息通信
 * 遵循最新的 message channel API 规范
 */
class PhoenixMessageClient private constructor() {
    private val TAG = "PhoenixMessageClient"

    // WebSocket connection
    private var socket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var authToken: String? = null
    var userId: String? = null
    private var gson: Gson = GsonConfig.createPrettyGson()

    // CoroutineScope for operations
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 连接状态机: disconnected -> connected -> joined
    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    // UnreadCounts state
    private val _unreadCounts = MutableLiveData<UnreadCounts>(UnreadCounts())
    val unreadCounts: LiveData<UnreadCounts> = _unreadCounts

    // Connection timeout
    private val CONNECT_TIMEOUT = 10L // seconds
    private val OPERATION_TIMEOUT = 15L // seconds

    // Channel state
    private var userChannel: String? = null

    // 是否保持连接
    private var keepConnected = false

    // Message flows for UI to observe
    private val _newMessages = MutableSharedFlow<Message>(replay = 0)
    val newMessages: SharedFlow<Message> = _newMessages

    private val _messageStatusChanges = MutableSharedFlow<MessageStatusChange>(replay = 0)
    val messageStatusChanges: SharedFlow<MessageStatusChange> = _messageStatusChanges

    // Error flow for connection issues
    private val _errors = MutableSharedFlow<PhoenixError>(replay = 0)
    val errors: SharedFlow<PhoenixError> = _errors

    // Request callbacks
    private val responseCallbacks = ConcurrentHashMap<String, PhoenixCallback>()

    // Counter for message references
    private val refCounter = AtomicInteger(0)

    // Heartbeat timer
    private var heartbeatTimer: Timer? = null

    // Last sync timestamp
    private var lastSyncTimestamp: String? = null

    // Auto-sync timer
    private var autoSyncTimer: Timer? = null
    private var isAutoSyncEnabled = false

    companion object {
        val instance: PhoenixMessageClient by lazy { PhoenixMessageClient() }

        private var BASE_SOCKET_URL = "ws://10.70.141.134:4000/socket/websocket"
        private const val HEARTBEAT_INTERVAL = 30000L // 30 seconds
        private const val AUTO_SYNC_INTERVAL = 60000L // 60 seconds

        // Phoenix channel events
        const val EVENT_JOIN = "phx_join"
        const val EVENT_LEAVE = "phx_leave"
        const val EVENT_REPLY = "phx_reply"
        const val EVENT_ERROR = "phx_error"
        const val EVENT_CLOSE = "phx_close"
        const val EVENT_HEARTBEAT = "heartbeat"

        // API事件，根据最新文档
        const val EVENT_SYNC = "sync"
        const val EVENT_SEND_MESSAGE = "send_message"
        const val EVENT_MARK_READ = "mark_read"
        const val EVENT_MARK_ALL_READ = "mark_all_read"
        const val EVENT_GET_MESSAGE_HISTORY = "get_message_history"
        const val EVENT_GET_UNREAD_STATS = "get_unread_stats"
    }

    /**
     * 配置WebSocket URL
     */
    fun configureUrl(socketUrl: String) {
        BASE_SOCKET_URL = socketUrl
    }

    /**
     * 连接 WebSocket 服务器
     * 增加同步锁和更严格的状态检查，避免重复连接
     */
    @Synchronized
    fun connect(authToken: String, userId: String) {
        // 更严格的连接状态检查
        val currentState = _connectionState.value
        if (currentState == ConnectionState.CONNECTING || currentState == ConnectionState.CONNECTED || currentState == ConnectionState.JOINED) {
            Log.d(TAG, "WebSocket已连接或正在连接中，当前状态: $currentState")
            return
        }

        this.authToken = authToken
        this.userId = userId
        keepConnected = true
        _connectionState.postValue(ConnectionState.CONNECTING)

        try {
            // 设置OkHttp客户端
            client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS).build()

            val url = buildWebSocketUrl()
            Log.d(TAG, "连接WebSocket: $url")

            // 创建请求
            val request =
                Request.Builder().url(url).addHeader("Authorization", "Bearer $authToken").build()

            // 建立WebSocket连接
            socket = client?.newWebSocket(request, createWebSocketListener())

        } catch (e: Exception) {
            handleConnectionError("连接失败: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    /**
     * 断开 WebSocket 连接
     */
    fun disconnect() {
        keepConnected = false

        // 取消定时器
        heartbeatTimer?.cancel()
        heartbeatTimer = null

        autoSyncTimer?.cancel()
        autoSyncTimer = null

        // 关闭WebSocket
        socket?.close(1000, "正常关闭")
        socket = null

        // 清理回调和状态
        responseCallbacks.clear()
        userChannel = null

        // 取消协程
        clientScope.coroutineContext.cancelChildren()

        // 更新状态
        _connectionState.postValue(ConnectionState.DISCONNECTED)
    }

    /**
     * 加入用户频道
     */
    suspend fun joinUserChannel(): Result<JSONObject> = runCatching {
        if (userId.isNullOrEmpty()) {
            throw PhoenixException("用户ID为空")
        }

        val channelName = "user:${userId}"
        userChannel = channelName

        val ref = generateRef()
        val payload = JSONObject().apply {
            put("token", authToken ?: throw PhoenixException("未提供认证令牌"))
        }

        val joinPayload = JSONObject().apply {
            put("topic", channelName)
            put("event", EVENT_JOIN)
            put("payload", payload)
            put("ref", ref)
        }

        Log.d(TAG, "正在加入频道: $channelName")
        val response = sendAndWaitForResponse(joinPayload, ref)
        Log.d(TAG, "加入频道响应: $response")

        // 成功加入后更新状态
        _connectionState.postValue(ConnectionState.JOINED)

        // 启动自动同步
//        if (isAutoSyncEnabled) {
//            startAutoSync()
//        }

        response
    }

    /**
     * 离开用户频道
     */
    suspend fun leaveUserChannel(): Result<JSONObject> = runCatching {
        if (userChannel == null) {
            throw PhoenixException("未加入用户频道")
        }

        val channelName = userChannel!!
        val ref = generateRef()
        val leavePayload = JSONObject().apply {
            put("topic", channelName)
            put("event", EVENT_LEAVE)
            put("payload", JSONObject())
            put("ref", ref)
        }

        val response = sendAndWaitForResponse(leavePayload, ref)

        // 更新状态为已连接但未加入频道
        _connectionState.postValue(ConnectionState.CONNECTED)

        // 停止自动同步
        stopAutoSync()
        userChannel = null

        response
    }

    /**
     * 发送消息
     * 根据最新API文档实现
     */
    suspend fun sendMessage(
        receiverId: String,
        content: List<MessageContentItem>,
        messageType: String = MessageTypes.CHAT
    ): Result<Message> = runCatching {
        checkUserChannel()

        // 客户端消息ID格式：{userId}-{timestamp}
        val clientMessageId = "${userId}-${System.currentTimeMillis()}"

        // 构建内容数组
        val contentArray = JSONArray()
        content.forEach { item ->
            val contentItem = JSONObject()
            contentItem.put("type", item.type)
            contentItem.put("payload", item.payload)
            contentArray.put(contentItem)
        }

        // 构建请求负载，符合新API
        val payload = JSONObject().apply {
            put("content", contentArray)
            put("receiver_id", receiverId)
            put("message_id", clientMessageId)
        }

        val response = push(userChannel!!, EVENT_SEND_MESSAGE, payload)

        // 解析响应
        val serverMessageId = response.optString("id")
        val returnedClientMessageId = response.optString("message_id")
        val serverTimestamp = response.optLong("server_timestamp")
        val status = response.optString("status", MessageStatus.SENT)

        if (serverMessageId.isEmpty()) {
            throw PhoenixException("消息发送失败: 响应格式错误")
        }

        // 使用服务器返回的时间戳创建Date对象
        val createdAt = if (serverTimestamp > 0) Date(serverTimestamp) else Date()

        // 构建消息对象
        val message = Message(
            id = serverMessageId,
            clientMessageId = returnedClientMessageId,
            senderId = userId!!,
            receiverId = receiverId,
            content = content,
            messageType = messageType,
            conversationId = getConversationId(userId!!, receiverId),
            status = status,
            createdAt = createdAt,
            updatedAt = createdAt
        )

        // 发布到消息流
        clientScope.launch {
            _newMessages.emit(message)
        }

        message
    }

    /**
     * 标记消息为已读
     * 根据最新API文档实现
     */
    suspend fun markMessagesRead(messageIds: List<String>): Result<Int> = runCatching {
        checkUserChannel()

        val messageIdsArray = JSONArray()
        messageIds.forEach { messageIdsArray.put(it) }

        val payload = JSONObject().apply {
            put("message_ids", messageIdsArray)
        }

        val response = push(userChannel!!, EVENT_MARK_READ, payload)
        response.optInt("updated_count", 0)
    }

    /**
     * 标记所有消息为已读
     * 根据最新API文档实现
     */
    suspend fun markAllMessagesRead(): Result<Int> = runCatching {
        checkUserChannel()

        val payload = JSONObject()
        val response = push(userChannel!!, EVENT_MARK_ALL_READ, payload)
        response.optInt("updated_count", 0)
    }

    /**
     * 同步消息
     * 根据最新API文档实现
     */
    suspend fun syncMessages(since: String? = null): Result<SyncResult> = runCatching {
        checkUserChannel()

        val timestamp = since ?: lastSyncTimestamp ?: "0"

        val payload = JSONObject().apply {
            put("last_sync_timestamp", timestamp)
        }

        val response = push(userChannel!!, EVENT_SYNC, payload)

        // 解析消息列表
        val messages = mutableListOf<Message>()
        val messagesArray = response.optJSONArray("messages") ?: JSONArray()

        for (i in 0 until messagesArray.length()) {
            val messageJson = messagesArray.getJSONObject(i)
            messages.add(gson.fromJson(messageJson.toString(), Message::class.java))
        }

        // 获取新的同步时间戳
        val newTimestamp = response.optString("new_last_sync_timestamp", timestamp)
        lastSyncTimestamp = newTimestamp

        // 构建同步结果
        SyncResult(messages, Date())
    }

    /**
     * 获取消息历史
     * 根据最新API文档实现
     */
    suspend fun getMessageHistory(
        limit: Int = 20, offset: Int = 0
    ): Result<List<Message>> = runCatching {
        checkUserChannel()

        val payload = JSONObject().apply {
            put("limit", limit)
            put("offset", offset)
        }

        val response = push(userChannel!!, EVENT_GET_MESSAGE_HISTORY, payload)

        // 解析消息列表
        val messages = mutableListOf<Message>()
        val messagesArray = response.optJSONArray("messages") ?: JSONArray()

        for (i in 0 until messagesArray.length()) {
            val messageJson = messagesArray.getJSONObject(i)
            messages.add(gson.fromJson(messageJson.toString(), Message::class.java))

        }

        messages
    }

    /**
     * 获取未读消息统计
     * 根据最新API文档实现
     */
    suspend fun getUnreadStats(): Result<UnreadCounts> = runCatching {
        checkUserChannel()

        val payload = JSONObject()
        val response = push(userChannel!!, EVENT_GET_UNREAD_STATS, payload)

        val stats = response.optJSONObject("stats") ?: JSONObject()

        UnreadCounts(
            total = stats.optInt("total", 0),
            chat = stats.optInt("chat", 0),
            system = stats.optInt("system", 0),
            order = stats.optInt("order", 0),
            payment = stats.optInt("payment", 0)
        )
    }

    /**
     * 启用自动同步
     */
    fun enableAutoSync(syncIntervalMs: Long = AUTO_SYNC_INTERVAL) {
        isAutoSyncEnabled = true

        // 如果已连接用户频道，立即启动自动同步
        if (userChannel != null && _connectionState.value == ConnectionState.JOINED) {
            startAutoSync(syncIntervalMs)
        }
    }

    /**
     * 禁用自动同步
     */
    fun disableAutoSync() {
        isAutoSyncEnabled = false
        stopAutoSync()
    }

    /**
     * 启动自动同步定时器
     */
    private fun startAutoSync(syncIntervalMs: Long = AUTO_SYNC_INTERVAL) {
        stopAutoSync() // 先停止现有定时器

        autoSyncTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (_connectionState.value == ConnectionState.JOINED && userChannel != null) {
                        clientScope.launch {
                            syncMessages().onFailure { error ->
                                Log.e(TAG, "自动同步失败", error)
                            }
                        }
                    }
                }
            }, syncIntervalMs, syncIntervalMs)
        }

        // 立即执行一次同步
        clientScope.launch {
            syncMessages().onFailure { error ->
                Log.e(TAG, "初始同步失败", error)
            }
        }
    }

    /**
     * 停止自动同步定时器
     */
    private fun stopAutoSync() {
        autoSyncTimer?.cancel()
        autoSyncTimer = null
    }

    /**
     * 构建WebSocket URL
     */
    private fun buildWebSocketUrl(): String {
        // 处理ws://和http://前缀
        val httpUrl = BASE_SOCKET_URL.replace(Regex("^ws://"), "http://")
        val urlBuilder = httpUrl.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("无效的WebSocket URL: $BASE_SOCKET_URL")

        return urlBuilder.build().toString().replace(Regex("^http://"), "ws://")
    }

    /**
     * 检查用户频道是否已加入
     */
    private fun checkUserChannel() {
        if (userChannel == null || _connectionState.value != ConnectionState.JOINED) {
            throw PhoenixException("未加入用户频道")
        }
    }

    /**
     * 向频道发送事件
     */
    private suspend fun push(channelName: String, event: String, payload: JSONObject): JSONObject {
        val ref = generateRef()
        val message = JSONObject().apply {
            put("topic", channelName)
            put("event", event)
            put("payload", payload)
            put("ref", ref)
        }

        return sendAndWaitForResponse(message, ref)
    }

    /**
     * 发送消息并等待响应
     */
    private suspend fun sendAndWaitForResponse(payload: JSONObject, ref: String): JSONObject =
        withTimeout(OPERATION_TIMEOUT * 1000) {
            suspendCancellableCoroutine { continuation ->
                responseCallbacks[ref] = PhoenixCallback(continuation)

                try {
                    Log.d(TAG, "发送: $payload")
                    socket?.send(payload.toString()) ?: run {
                        responseCallbacks.remove(ref)
                        throw PhoenixException("WebSocket未连接")
                    }
                } catch (e: Exception) {
                    responseCallbacks.remove(ref)
                    throw e
                }

                continuation.invokeOnCancellation {
                    responseCallbacks.remove(ref)
                }
            }
        }

    /**
     * 生成唯一引用ID
     */
    private fun generateRef(): String {
        return refCounter.incrementAndGet().toString()
    }

    /**
     * 启动心跳定时器
     */
    private fun startHeartbeat() {
        heartbeatTimer?.cancel()

        heartbeatTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.JOINED) {
                        val ref = generateRef()
                        val payload = JSONObject().apply {
                            put("topic", "phoenix")
                            put("event", EVENT_HEARTBEAT)
                            put("payload", JSONObject())
                            put("ref", ref)
                        }
                        socket?.send(payload.toString())
                    }
                }
            }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL)
        }
    }

    /**
     * 处理连接错误
     */
    private fun handleConnectionError(message: String) {
        Log.e(TAG, "WebSocket连接错误: $message")

        _connectionState.postValue(ConnectionState.CONNECTION_ERROR)

        clientScope.launch {
            _errors.emit(PhoenixError("connection_error", message))
        }
    }

    /**
     * 创建WebSocket监听器
     */
    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket连接已打开")
                _connectionState.postValue(ConnectionState.CONNECTED)
                startHeartbeat()

                // 重新加入用户频道
                if (userChannel != null) {
                    clientScope.launch {
                        try {
                            joinUserChannel()
                        } catch (e: Exception) {
                            Log.e(TAG, "重新加入用户频道失败", e)
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = JSONObject(text)
                    handleWebSocketMessage(message)
                } catch (e: Exception) {
                    Log.e(TAG, "解析消息失败", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket正在关闭: $code, $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket已关闭: $code, $reason")
                _connectionState.postValue(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = t.localizedMessage ?: "未知错误"
                Log.e(TAG, "WebSocket失败: $errorMsg", t)
                handleConnectionError(errorMsg)
            }
        }
    }

    /**
     * 处理WebSocket消息
     */
    private fun handleWebSocketMessage(message: JSONObject) {
        val topic = message.optString("topic")
        val event = message.optString("event")
        val payload = message.optJSONObject("payload") ?: JSONObject()
        val ref = message.optString("ref")

        Log.d(TAG, "接收到消息: $message")

        when (event) {
            EVENT_REPLY -> handleReply(ref, payload)
            EVENT_ERROR -> handleChannelError(topic, payload)
            "new_message" -> handleIncomingMessage(payload) // 处理服务器推送的新消息
            // 这里添加了对未读计数更新事件的处理
            "unread_count_update" -> handleUnreadCountUpdate(payload)
            else -> Log.d(TAG, "未处理的事件类型: $event")
        }
    }

    /**
     * 处理通道错误
     */
    private fun handleChannelError(topic: String, payload: JSONObject) {
        Log.e(TAG, "通道错误: $topic, $payload")

        // 如果是用户通道出错，将状态从JOINED设置回CONNECTED
        if (topic == userChannel) {
            _connectionState.postValue(ConnectionState.CONNECTED)

            // 尝试重新加入
            clientScope.launch {
                try {
                    joinUserChannel()
                } catch (e: Exception) {
                    Log.e(TAG, "重新加入用户频道失败", e)
                }
            }
        }
    }

    /**
     * 处理服务器回复
     */
    private fun handleReply(ref: String, payload: JSONObject) {
        if (ref.isNotEmpty() && responseCallbacks.containsKey(ref)) {
            val callback = responseCallbacks[ref]
            responseCallbacks.remove(ref)

            val status = payload.optString("status")
            val response = payload.optJSONObject("response") ?: JSONObject()

            if (status == "error") {
                val reason = response.optString("reason", "未知错误")
                callback?.continuation?.resumeWithException(PhoenixException(reason))
            } else {
                callback?.continuation?.resume(response)
            }
        }
    }

    /**
     * 处理服务器推送的消息
     */
    private fun handleIncomingMessage(payload: JSONObject) {
        clientScope.launch {
            try {
                val message = gson.fromJson(
                    payload.getJSONObject("message").toString(), Message::class.java
                )


                Log.d(TAG, "接收到新消息: $message")

                // 发布到新消息流
                _newMessages.emit(message)

                // 更新未读计数
                updateUnreadCounts(message.messageType)
            } catch (e: Exception) {
                Log.e(TAG, "处理消息失败", e)
            }
        }
    }

    /**
     * 处理未读计数更新事件
     */
    private fun handleUnreadCountUpdate(payload: JSONObject) {
        try {
            val stats = payload.optJSONObject("stats") ?: JSONObject()
            val newCounts = UnreadCounts(
                total = stats.optInt("total", 0),
                chat = stats.optInt("chat", 0),
                system = stats.optInt("system", 0),
                order = stats.optInt("order", 0),
                payment = stats.optInt("payment", 0)
            )
            _unreadCounts.postValue(newCounts)
        } catch (e: Exception) {
            Log.e(TAG, "处理未读计数更新失败", e)
        }
    }

    /**
     * 更新未读消息计数
     */
    private fun updateUnreadCounts(messageType: String) {
        val currentCounts = _unreadCounts.value ?: UnreadCounts()
        val newCounts = when (messageType) {
            MessageTypes.CHAT -> currentCounts.copy(
                chat = currentCounts.chat + 1, total = currentCounts.total + 1
            )

            MessageTypes.SYSTEM -> currentCounts.copy(
                system = currentCounts.system + 1, total = currentCounts.total + 1
            )

            MessageTypes.ORDER -> currentCounts.copy(
                order = currentCounts.order + 1, total = currentCounts.total + 1
            )

            MessageTypes.PAYMENT -> currentCounts.copy(
                payment = currentCounts.payment + 1, total = currentCounts.total + 1
            )

            else -> currentCounts
        }
        _unreadCounts.postValue(newCounts)
    }

    /**
     * Phoenix 回调类
     */
    private class PhoenixCallback(
        val continuation: kotlinx.coroutines.CancellableContinuation<JSONObject>
    )

    /**
     * Phoenix 异常类
     */
    class PhoenixException(message: String) : Exception(message)
}

/**
 * 连接状态枚举 - 符合状态机模型
 */
enum class ConnectionState {
    DISCONNECTED,   // 未连接
    CONNECTING,     // 正在连接
    CONNECTED,      // 已连接但未加入频道
    JOINED,         // 已加入频道
    CONNECTION_ERROR // 连接错误
}

/**
 * 消息状态变更
 */
data class MessageStatusChange(
    val messageId: String, val oldStatus: String?, val newStatus: String
)

/**
 * Phoenix错误
 */
data class PhoenixError(
    val code: String, val message: String, val details: Any? = null
)


suspend fun navigateToChat(userId: String, k: (String) -> Unit) {
    k("/messages/$userId")
}