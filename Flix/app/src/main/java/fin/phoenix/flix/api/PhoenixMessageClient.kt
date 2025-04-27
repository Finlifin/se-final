package fin.phoenix.flix.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.ConversationDetail
import fin.phoenix.flix.data.ConversationTypes
import fin.phoenix.flix.data.ConversationUserSettings
import fin.phoenix.flix.data.EventTypes
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.MessageEvent
import fin.phoenix.flix.data.MessageStatus
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.SyncResult
import fin.phoenix.flix.data.UnreadCounts
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
import java.time.Clock
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Phoenix WebSocket 客户端，用于处理二手交易平台的实时消息通信
 * 基于 Phoenix 框架的 WebSocket 协议实现
 */
class PhoenixMessageClient private constructor() {
    private val TAG = "PhoenixMessageClient"

    // WebSocket connection
    private var socket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var authToken: String? = null
    var userId: String? = null
    private var gson = GsonConfig.createPrettyGson()

    // CoroutineScope for operations
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Connection state
    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    // UnreadCounts state
    private val _unreadCounts = MutableLiveData<UnreadCounts>(UnreadCounts())
    val unreadCounts: LiveData<UnreadCounts> = _unreadCounts

    // Connection timeout
    private val CONNECT_TIMEOUT = 10L // seconds
    private val OPERATION_TIMEOUT = 15L // seconds

    // Channel state
    private var joinedChannels = ConcurrentHashMap<String, String>() // channel -> ref
    private var userChannel: String? = null

    // 是否保持连接
    private var keepConnected = false

    // Message flows for UI to observe
    private val _messageEvents = MutableSharedFlow<MessageEvent>(replay = 0)
    val messageEvents: SharedFlow<MessageEvent> = _messageEvents

    private val _newMessages = MutableSharedFlow<Message>(replay = 0)
    val newMessages: SharedFlow<Message> = _newMessages

    private val _messageStatusChanges = MutableSharedFlow<MessageStatusChange>(replay = 0)
    val messageStatusChanges: SharedFlow<MessageStatusChange> = _messageStatusChanges

    // Error flow for connection issues
    private val _errors = MutableSharedFlow<PhoenixError>(replay = 0)
    val errors: SharedFlow<PhoenixError> = _errors

    // 重连相关配置
    private var reconnectTimer: Timer? = null
    private var reconnectAttempt = AtomicInteger(0)
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 2000L // 2秒基础延迟

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

//        private var BASE_SOCKET_URL = "ws://192.168.31.117:4000/socket/websocket"
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

        // Custom events
        const val EVENT_NEW_MESSAGE = "new_message"
        const val EVENT_MESSAGE_STATUS_CHANGED = "message_status_changed"
        const val EVENT_SEND_MESSAGE = "send_private_message"
        const val EVENT_MARK_READ = "mark_read"
        const val EVENT_WITHDRAW_MESSAGE = "withdraw_message"
        const val EVENT_SYNC = "sync"
        const val EVENT_GET_CONVERSATION_HISTORY = "get_conversation_history"
        const val EVENT_CREATE_CONVERSATION = "create_conversation"
        const val EVENT_GET_CONVERSATIONS = "get_conversations"
        const val EVENT_GET_CONVERSATION_INFO = "get_conversation_info"
        const val CLEAR_CONVERSATION = "clear_conversation"
    }


    /**
     * 配置WebSocket URL
     */
    fun configureUrl(socketUrl: String) {
        BASE_SOCKET_URL = socketUrl
    }

    /**
     * 连接 WebSocket 服务器
     */
    fun connect(authToken: String, userId: String) {
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
            Log.d(TAG, "WebSocket已连接或正在连接中")
            return
        }

        this.authToken = authToken
        this.userId = userId
        keepConnected = true
        reconnectAttempt.set(0)
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

        reconnectTimer?.cancel()
        reconnectTimer = null

        autoSyncTimer?.cancel()
        autoSyncTimer = null

        // 关闭WebSocket
        socket?.close(1000, "正常关闭")
        socket = null

        // 清理回调和状态
        responseCallbacks.clear()
        joinedChannels.clear()
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
        joinChannel(channelName)
    }

    /**
     * 加入指定频道
     */
    private suspend fun joinChannel(channelName: String): JSONObject {
        if (joinedChannels.containsKey(channelName)) {
            Log.d(TAG, "已加入频道: $channelName")
            return JSONObject()
        }

        val ref = generateRef()
        val payload = JSONObject()

        // 添加身份认证令牌
        if (authToken != null) {
            payload.put("token", authToken)
        } else {
            throw PhoenixException("未提供认证令牌")
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
        // 检查响应状态
        val status = response.optString("status")

        joinedChannels[channelName] = ref
        Log.d(TAG, "成功加入频道: $channelName")

        // 如果是用户频道，启动自动同步
        if (channelName == userChannel && isAutoSyncEnabled) {
            startAutoSync()
        }


        return response
    }

    /**
     * 离开指定频道
     */
    suspend fun leaveUserChannel(): Result<JSONObject> = runCatching {
        if (userChannel == null) {
            throw PhoenixException("未加入用户频道")
        }

        leaveChannel(userChannel!!)
    }

    /**
     * 离开指定频道
     */
    private suspend fun leaveChannel(channelName: String): JSONObject {
        if (!joinedChannels.containsKey(channelName)) {
            throw PhoenixException("未加入该频道")
        }

        val ref = generateRef()
        val leavePayload = JSONObject().apply {
            put("topic", channelName)
            put("event", EVENT_LEAVE)
            put("payload", JSONObject())
            put("ref", ref)
        }

        val response = sendAndWaitForResponse(leavePayload, ref)
        joinedChannels.remove(channelName)

        // 如果离开的是用户频道，停止自动同步
        if (channelName == userChannel) {
            stopAutoSync()
            userChannel = null
        }

        return response
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        conversationId: String,
        content: List<MessageContentItem>,
        messageType: String = MessageTypes.PRIVATE_MESSAGE
    ): Result<Message> = runCatching {
        checkUserChannel()

        val clientMessageId = UUID.randomUUID().toString()
        val clientTimestamp = Clock.systemUTC()

        // 构建内容数组
        val contentArray = JSONArray()
        content.forEach { item ->
            val contentItem = JSONObject()
            contentItem.put("type", item.type)

            // 根据类型序列化载荷
            when (item.payload) {
                is String -> contentItem.put("payload", item.payload)
                is JSONObject -> contentItem.put("payload", item.payload)
                is Map<*, *> -> contentItem.put("payload", JSONObject(item.payload))
                else -> contentItem.put("payload", gson.toJson(item.payload))
            }

            contentArray.put(contentItem)
        }

        // 构建请求负载
        val payload = JSONObject().apply {
            put("client_message_id", clientMessageId)
            put("client_timestamp", clientTimestamp.toString())
            put("conversation_id", conversationId)
            put("content", contentArray)
            put("message_type", messageType)
        }

        val response = push(userChannel!!, EVENT_SEND_MESSAGE, payload)

        // 解析响应
        if (!response.has("client_message_id")) {
            throw PhoenixException("消息发送失败: 响应格式错误")
        }

        // 构建消息对象
        val message = buildMessageFromResponse(response, content, messageType, conversationId)

        // 发布到消息流
        clientScope.launch {
            _newMessages.emit(message)
        }

        message
    }

    /**
     * 撤回消息
     */
    suspend fun withdrawMessage(messageId: String): Result<JSONObject> = runCatching {
        checkUserChannel()

        val payload = JSONObject().apply {
            put("message_id", messageId)
        }

        push(userChannel!!, EVENT_WITHDRAW_MESSAGE, payload)
    }

    /**
     * 标记消息为已读
     */
    suspend fun markMessagesRead(
        conversationId: String, lastReadMessageId: String
    ): Result<JSONObject> = runCatching {
        checkUserChannel()

        val payload = JSONObject().apply {
            put("conversation_id", conversationId)
            put("last_read_message_id", lastReadMessageId)
        }

        push(userChannel!!, EVENT_MARK_READ, payload)
    }

    /**
     * 同步消息和事件
     */
    suspend fun syncMessages(since: String? = null): Result<SyncResult> = runCatching {
        checkUserChannel()

        val timestamp = since ?: lastSyncTimestamp ?: "2020-01-01T00:00:00.000Z"

        val payload = JSONObject().apply {
            put("last_sync_timestamp", timestamp)
        }

        val response = push(userChannel!!, EVENT_SYNC, payload)

        // 解析事件列表
        val events = mutableListOf<MessageEvent>()
        val eventsArray = response.optJSONArray("events") ?: JSONArray()

        for (i in 0 until eventsArray.length()) {
            val eventJson = eventsArray.getJSONObject(i)
            // 这里需要进一步实现从JSON到MessageEvent的转换
            // events.add(parseMessageEvent(eventJson)) 
        }

        // 获取新的同步时间戳
        val newTimestamp = response.optString("new_last_sync_timestamp", timestamp)
        lastSyncTimestamp = newTimestamp

        // 构建同步结果
        SyncResult(events, Date()) // 临时用当前日期代替，实际应从JSON解析
    }

    /**
     * 获取会话历史记录
     */
    suspend fun getConversationHistory(
        conversationId: String, limit: Int = 20, before: String? = null
    ): Result<List<Message>> = runCatching {
        checkUserChannel()

        val payload = JSONObject().apply {
            put("conversation_id", conversationId)
            put("limit", limit)
            if (before != null) {
                put("before", before)
            }
        }

        val response = push(userChannel!!, EVENT_GET_CONVERSATION_HISTORY, payload)

        // 解析消息列表
        val messages = mutableListOf<Message>()
        val messagesArray = response.optJSONArray("messages") ?: JSONArray()

        for (i in 0 until messagesArray.length()) {
            val messageJson = messagesArray.getJSONObject(i)
            // 这里需要进一步实现从JSON到Message的转换
            // messages.add(parseMessage(messageJson))
        }

        messages
    }

    /**
     * 创建会话
     */
    suspend fun createConversation(
        type: String, participantIds: List<String>
    ): Result<ConversationDetail> = runCatching {
        checkUserChannel()

        val participantsArray = JSONArray()
        participantIds.forEach { participantsArray.put(it) }

        val payload = JSONObject().apply {
            put("type", type)
            put("participant_ids", participantsArray)
        }

        val response = push(userChannel!!, EVENT_CREATE_CONVERSATION, payload)

        // 解析会话
        val conversationJson = response.optJSONObject("conversation")
            ?: throw PhoenixException("创建会话失败: 响应格式错误")

        // 解析基本会话信息
        val conversation = Conversation(
            id = conversationJson.getString("id"),
            conversationId = conversationJson.getString("conversation_id"),
            type = conversationJson.getString("type"),
            participantIds = List(conversationJson.getJSONArray("participant_ids").length()) { i ->
                conversationJson.getJSONArray("participant_ids").getString(i)
            },
            lastMessageId = conversationJson.optString("last_message_id"),
            lastMessageContent = conversationJson.optString("last_message_content"),
            lastMessageTimestamp = null, // 需要解析时间戳
            updatedAt = Date(), // 需要解析时间戳
            insertedAt = Date() // 需要解析时间戳
        )

        // 创建用户设置对象
        val userSettings = ConversationUserSettings(
            unreadCount = response.optInt("unread_count", 0),
            isPinned = response.optBoolean("is_pinned", false),
            isMuted = response.optBoolean("is_muted", false),
            lastReadMessageId = response.optString("last_read_message_id"),
            draft = null
        )

        // 返回会话详情
        ConversationDetail(conversation, userSettings)
    }

    suspend fun getOrCreateConversationWith(with: String): Result<ConversationDetail> {
        return createConversation(ConversationTypes.PRIVATE, listOf(userId!!, with))
    }

    suspend fun getConversation(
        conversationId: String
    ): Result<ConversationDetail> = runCatching {
        checkUserChannel()

//        val payload = JSONObject().apply {
//            put("conversation_id", conversationId)
//        }
        val payload = JSONObject(mapOf("conversation_id" to conversationId))
        Log.d(TAG, "获取会话详情请求: $payload")

        val response = push(userChannel!!, EVENT_GET_CONVERSATION_INFO, payload)

        // 解析会话详情
        val conversationJson = response.optJSONObject("conversation")
            ?: throw PhoenixException("获取会话失败: 响应格式错误")

        // 解析基本会话信息
        val conversation = Conversation(
            id = conversationJson.getString("id"),
            conversationId = conversationJson.getString("conversation_id"),
            type = conversationJson.getString("type"),
            participantIds = List(conversationJson.getJSONArray("participant_ids").length()) { i ->
                conversationJson.getJSONArray("participant_ids").getString(i)
            },
            lastMessageId = conversationJson.optString("last_message_id"),
            lastMessageContent = conversationJson.optString("last_message_content"),
            lastMessageTimestamp = null, // 需要解析时间戳
            updatedAt = Date(), // 需要解析时间戳
            insertedAt = Date() // 需要解析时间戳
        )

        // 创建用户设置对象
        val userSettings = ConversationUserSettings(
            unreadCount = response.optInt("unread_count", 0),
            isPinned = response.optBoolean("is_pinned", false),
            isMuted = response.optBoolean("is_muted", false),
            lastReadMessageId = response.optString("last_read_message_id"),
            draft = null
        )

        // 返回会话详情
        ConversationDetail(conversation, userSettings)
    }

    /**
     * 获取会话列表
     */
    suspend fun getConversations(limit: Int = 20): Result<List<ConversationDetail>> = runCatching {
        checkUserChannel()

        val payload = JSONObject().apply {
            put("limit", limit)
        }

        val response = push(userChannel!!, EVENT_GET_CONVERSATIONS, payload)
        Log.d(TAG, "获取会话列表响应: $response")

        // 解析会话列表
        val conversations = mutableListOf<ConversationDetail>()
        val responseArray = if (response.has("conversations")) {
            response.getJSONArray("conversations")
        } else {
            response as? JSONArray ?: throw PhoenixException("获取会话失败: 响应格式错误")
        }

        for (i in 0 until responseArray.length()) {
            val item = responseArray.getJSONObject(i)
            val conversationJson = item.optJSONObject("conversation") ?: item

            // 解析基本会话信息
            val conversation = Conversation(
                id = conversationJson.getString("id"),
                conversationId = conversationJson.getString("conversation_id"),
                type = conversationJson.getString("type"),
                participantIds = List(
                    conversationJson.getJSONArray("participant_ids").length()
                ) { j ->
                    conversationJson.getJSONArray("participant_ids").getString(j)
                },
                lastMessageId = conversationJson.optString("last_message_id"),
                lastMessageContent = conversationJson.optString("last_message_content"),
                lastMessageTimestamp = null, // 需要解析时间戳
                updatedAt = Date(), // 需要解析时间戳
                insertedAt = Date() // 需要解析时间戳
            )

            // 创建用户设置对象
            val userSettings = ConversationUserSettings(
                unreadCount = item.optInt("unread_count", 0),
                isPinned = item.optBoolean("is_pinned", false),
                isMuted = item.optBoolean("is_muted", false),
                lastReadMessageId = item.optString("last_read_message_id"),
                draft = null
            )

            conversations.add(ConversationDetail(conversation, userSettings))
        }

        conversations
    }

    /*
        * 清除会话
     */
    suspend fun clearConversation(
        conversationId: String
    ): Result<JSONObject> = runCatching {
        checkUserChannel()

        val payload = JSONObject(mapOf("conversation_id" to conversationId))

        val response = push(userChannel!!, CLEAR_CONVERSATION, payload)
        Log.d(TAG, "清除会话响应: $response")

        response
    }

    /**
     * 启用自动同步
     */
    fun enableAutoSync(syncIntervalMs: Long = AUTO_SYNC_INTERVAL) {
        isAutoSyncEnabled = true

        // 如果已连接用户频道，立即启动自动同步
        if (userChannel != null && _connectionState.value == ConnectionState.CONNECTED) {
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
                    if (_connectionState.value == ConnectionState.CONNECTED && userChannel != null) {
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
        if (userChannel == null || !joinedChannels.containsKey(userChannel!!)) {
            throw PhoenixException("未加入用户频道")
        }
    }

    /**
     * 向频道发送事件
     */
    private suspend fun push(channelName: String, event: String, payload: JSONObject): JSONObject {
        if (!joinedChannels.containsKey(channelName)) {
            throw PhoenixException("未加入频道: $channelName")
        }

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
                    if (_connectionState.value == ConnectionState.CONNECTED) {
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

        scheduleReconnect()
    }

    /**
     * 安排重连
     */
    private fun scheduleReconnect() {
        if (!keepConnected) {
            return
        }

        val currentAttempt = reconnectAttempt.incrementAndGet()
        if (currentAttempt > maxReconnectAttempts) {
            Log.e(TAG, "达到最大重连次数")
            clientScope.launch {
                _errors.emit(
                    PhoenixError(
                        "max_reconnect_attempts", "达到最大重连次数 ($maxReconnectAttempts)"
                    )
                )
            }
            return
        }

        reconnectTimer?.cancel()
        val delay = calculateReconnectDelay(currentAttempt)

        Log.d(TAG, "安排重连, 延迟 $delay ms, 尝试 #$currentAttempt")
        _connectionState.postValue(ConnectionState.RECONNECTING)

        reconnectTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (keepConnected) {
                        authToken?.let { token ->
                            userId?.let { uid ->
                                connect(token, uid)
                            }
                        }
                    }
                }
            }, delay)
        }
    }

    /**
     * 计算重连延迟
     */
    private fun calculateReconnectDelay(attempt: Int): Long {
        return (baseReconnectDelay * (1 shl (attempt - 1))).coerceAtMost(30000L) // 最大30秒
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
                if (userChannel != null && !joinedChannels.containsKey(userChannel!!)) {
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
                handleWebSocketClosure(code, reason)
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

        when (event) {
            EVENT_REPLY -> handleReply(ref, payload)
            "event" -> handleEvent(payload)
            else -> Log.d(TAG, "未处理的事件类型: $event")
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
     * 处理服务器事件
     */
    private fun handleEvent(payload: JSONObject) {
        val eventType = payload.optString("event_type")

        when (eventType) {
            EventTypes.NEW_MESSAGE -> handleNewMessageEvent(payload.optJSONObject("payload")!!)
            EventTypes.MESSAGE_STATUS_CHANGED -> handleMessageStatusEvent(payload)
            else -> {
                // 发布原始事件
                clientScope.launch {
                    // 这里需要进一步实现从JSON到MessageEvent的转换
                    // val event = parseMessageEvent(payload)
//                     _messageEvents.emit(event)
                }
            }
        }
    }

    /**
     * 处理新消息事件
     */
    private fun handleNewMessageEvent(payload: JSONObject) {
        clientScope.launch {
            try {
                // 这里需要进一步实现从JSON到Message的转换
                val message = parseMessage(payload)
                Log.d(TAG, "处理新消息事件: $message")

                // 发布到新消息流
                _newMessages.emit(message)

                // 更新未读计数
                updateUnreadCounts(
                    payload.optJSONObject("payload")?.optString("message_type")
                        ?: MessageTypes.PRIVATE_MESSAGE
                )
            } catch (e: Exception) {
                Log.e(TAG, "处理新消息事件失败", e)
            }
        }
    }

    /**
     * 解析消息
     */
    private fun parseMessage(message: JSONObject): Message {
        val gson = GsonConfig.createPrettyGson()
        return gson.fromJson(message.toString(), Message::class.java)
    }

    /**
     * 处理消息状态变更事件
     */
    private fun handleMessageStatusEvent(payload: JSONObject) {
        clientScope.launch {
            try {
                val messagePayload = payload.optJSONObject("payload") ?: return@launch
                val messageId = messagePayload.optString("message_id")
                val oldStatus = messagePayload.optString("old_status")
                val newStatus = messagePayload.optString("new_status")

                if (messageId.isNotEmpty() && newStatus.isNotEmpty()) {
                    _messageStatusChanges.emit(
                        MessageStatusChange(
                            messageId = messageId,
                            oldStatus = oldStatus.takeIf { it.isNotEmpty() },
                            newStatus = newStatus
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理消息状态事件失败", e)
            }
        }
    }

    /**
     * 更新未读消息计数
     */
    fun updateUnreadCounts(messageType: String) {
        val currentCounts = _unreadCounts.value ?: UnreadCounts()
        val newCounts = when (messageType) {
            MessageTypes.PRIVATE_MESSAGE -> currentCounts.copy(
                privateMessage = currentCounts.privateMessage + 1, total = currentCounts.total + 1
            )

            MessageTypes.SYSTEM_NOTIFICATION -> currentCounts.copy(
                systemNotification = currentCounts.systemNotification + 1,
                total = currentCounts.total + 1
            )

            MessageTypes.SYSTEM_ANNOUNCEMENT -> currentCounts.copy(
                systemAnnouncement = currentCounts.systemAnnouncement + 1,
                total = currentCounts.total + 1
            )

            MessageTypes.INTERACTION -> currentCounts.copy(
                interaction = currentCounts.interaction + 1, total = currentCounts.total + 1
            )

            else -> currentCounts
        }
        _unreadCounts.postValue(newCounts)
    }

    /**
     * 处理WebSocket关闭
     */
    private fun handleWebSocketClosure(code: Int, reason: String) {
        heartbeatTimer?.cancel()
        heartbeatTimer = null

        if (keepConnected && code != 1000) { // 1000是正常关闭
            scheduleReconnect()
        }
    }

    /**
     * 构建消息对象
     */
    private fun buildMessageFromResponse(
        response: JSONObject,
        content: List<MessageContentItem>,
        messageType: String,
        conversationId: String
    ): Message {
        val clientMessageId = response.optString("client_message_id")
        val messageId = response.optString("message_id")
        val serverTimestamp = response.optString("server_timestamp")
        val status = response.optString("status", MessageStatus.SENT)

        return Message(
            id = messageId,
            clientMessageId = clientMessageId,
            senderId = userId,
            receiverId = null, // 需要补充
            conversationId = conversationId,
            content = content,
            messageType = messageType,
            status = status,
            clientTimestamp = Date(),
            serverTimestamp = Date(), // 需要解析serverTimestamp
            insertedAt = Date(), // 需要解析serverTimestamp
            updatedAt = Date() // 需要解析serverTimestamp
        )
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
 * 连接状态枚举
 */
enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, CONNECTION_ERROR
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
    val result = PhoenixMessageClient.instance.getOrCreateConversationWith(userId)
    val conversation = result.getOrThrow()
    k("/messages/${conversation.conversation.conversationId}")
}