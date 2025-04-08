package fin.phoenix.flix.api

import android.content.Context
import android.util.Log
import fin.phoenix.flix.api.PhoenixMessageClient.PhoenixMessage
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Phoenix WebSocket 客户端，用于处理二手交易平台的实时消息通信
 * 基于 Phoenix 框架的 WebSocket 协议实现
 */
class PhoenixMessageClient() {
    private val TAG = "PhoenixMessageClient"

    // WebSocket connection
    private var socket: WebSocket? = null
    private var client: OkHttpClient? = null

    // Connection timeout
    private val CONNECT_TIMEOUT = 10L // seconds
    private val OPERATION_TIMEOUT = 15L // seconds

    // Channel state
    var isConnected = false
        private set
    private var joinedChannels = mutableMapOf<String, String>() // channel -> ref

    // Message flow for UI to observe
    private val _messageFlow = MutableSharedFlow<PhoenixMessage>(replay = 0)
    val messageFlow: SharedFlow<PhoenixMessage> = _messageFlow

    // Error flow for connection issues
    private val _errorFlow = MutableSharedFlow<String>(replay = 0)
    val errorFlow: SharedFlow<String> = _errorFlow

    // 重连相关配置
    private var shouldReconnect = true
    private var reconnectTimer: Timer? = null
    private var reconnectAttempt = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 2000L // 2秒基础延迟

    /**
     * 发送错误消息到Flow
     */
    internal suspend fun emit(error: String) {
        _errorFlow.emit(error)
    }

    // Response listeners
    private val responseCallbacks = mutableMapOf<String, CompletableCallback>()

    // Counter for message references
    private var refCounter = 0

    // Heartbeat timer
    private var heartbeatTimer: Timer? = null

    companion object {
        lateinit var instance: PhoenixMessageClient

        private const val SOCKET_URL = "ws://192.168.31.117:4000/socket/websocket"
        private const val HEARTBEAT_INTERVAL = 30000L // 30 seconds

        // Phoenix channel events
        const val EVENT_JOIN = "phx_join"
        const val EVENT_LEAVE = "phx_leave"
        const val EVENT_HEARTBEAT = "heartbeat"

        // Message types
        const val MESSAGE_TYPE_SYSTEM_NOTIFICATION = "system_notification"
        const val MESSAGE_TYPE_SYSTEM_ANNOUNCEMENT = "system_announcement"
        const val MESSAGE_TYPE_INTERACTION = "interaction"
        const val MESSAGE_TYPE_PRIVATE_MESSAGE = "private_message"

        // Message status
        const val MESSAGE_STATUS_UNREAD = "unread"
        const val MESSAGE_STATUS_READ = "read"
        const val MESSAGE_STATUS_DELETED = "deleted"

        // Content types
        const val CONTENT_TYPE_TEXT = "text"
        const val CONTENT_TYPE_IMAGE = "image"
        const val CONTENT_TYPE_PRODUCT = "product"
        const val CONTENT_TYPE_ORDER = "order"
        const val CONTENT_TYPE_COMMENT = "comment"
        const val CONTENT_TYPE_LIKE = "like"
        const val CONTENT_TYPE_FAVORITE = "favorite"
        const val CONTENT_TYPE_SYSTEM = "system"

        // Custom events
        const val EVENT_NEW_MESSAGE = "new_message"
        const val EVENT_MESSAGE_STATUS_CHANGED = "message_status_changed"
        const val EVENT_MESSAGES_MARKED_READ = "messages_marked_read"
        const val EVENT_UNREAD_COUNT_UPDATE = "unread_count_update"
        const val EVENT_SEND_PRIVATE_MESSAGE = "send_private_message"
        const val EVENT_ACK_MESSAGE = "ack_message"
        const val EVENT_ACK_MESSAGES = "ack_messages"
        const val EVENT_GET_HISTORY = "get_history"
        const val EVENT_SYNC_MESSAGES = "sync_messages"
    }

    // Data class to hold callbacks
    private class CompletableCallback(
        val continuation: CancellableContinuation<JSONObject>
    )

    /**
     * 连接 WebSocket 服务器，带重试机制
     */
    fun connect(authToken: String): Boolean {
        if (isConnected) return true

        try {
            // 重置重连计数
            if (reconnectAttempt == 0) {
                shouldReconnect = true
            }

            // Set up OkHttp client with timeouts
            client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS).build()

            val url = buildWebSocketUrl(authToken)
            Log.d(TAG, "Connecting to WebSocket URL: $url")

            // Create request with auth header
            val request = Request.Builder().url(url).apply {
                addHeader("Authorization", "Bearer $authToken")
            }.build()

            // Set up WebSocket connection
            socket = client?.newWebSocket(request, createWebSocketListener())

            // Reset reconnect attempt counter on successful connection attempt
            reconnectAttempt = 0

            isConnected = true
            return true
        } catch (e: Exception) {
            handleConnectionError(e)
            return false
        }
    }

    /**
     * 重新连接 WebSocket
     */
    fun reconnect(authToken: String): Boolean {
        if (isConnected) {
            Log.d(TAG, "Reconnecting to WebSocket...")
            disconnect() // Disconnect first
            return connect(authToken) // Attempt to reconnect
        }

        isConnected = false
        return connect(authToken)
    }

    /**
     * 构建WebSocket URL
     */
    private fun buildWebSocketUrl(authToken: String?): String {
        return if (authToken != null) {
            // Convert ws:// to http:// temporarily for parsing
            val httpUrl = SOCKET_URL.replace("ws://", "http://")
            val urlBuilder = httpUrl.toHttpUrlOrNull()?.newBuilder()
                ?: throw IllegalArgumentException("Invalid WebSocket URL: $SOCKET_URL")
            // Convert back to ws:// for the actual connection
            urlBuilder.build().toString().replace("http://", "ws://")
        } else {
            SOCKET_URL
        }
    }

    /**
     * 处理连接错误
     */
    private fun handleConnectionError(error: Throwable) {
        Log.e(TAG, "WebSocket connection error", error)
        isConnected = false

        // Emit error to flow for UI to observe
        CoroutineScope(Dispatchers.IO).launch {
            _errorFlow.emit("连接失败: ${error.localizedMessage ?: "未知错误"}")
        }
    }

    /**
     * 安排重连
     */
//    private fun scheduleReconnect() {
//        if (reconnectAttempt >= maxReconnectAttempts) {
//            Log.e(TAG, "达到最大重连次数")
//            shouldReconnect = false
//            return
//        }
//
//        reconnectTimer?.cancel()
//        reconnectTimer = Timer().apply {
//            val delay = calculateReconnectDelay()
//            Log.d(TAG, "安排重连，延迟 $delay ms, 尝试 #${reconnectAttempt + 1}")
//
//            schedule(object : TimerTask() {
//                override fun run() {
//                    reconnectAttempt++
//                    connect()
//                }
//            }, delay)
//        }
//    }

    /**
     * 计算重连延迟时间（指数退避）
     */
    private fun calculateReconnectDelay(): Long {
        return (baseReconnectDelay * (1 shl reconnectAttempt)).coerceAtMost(30000L) // 最大30秒
    }

    /**
     * 断开 WebSocket 连接
     */
    fun disconnect() {
        heartbeatTimer?.cancel()
        heartbeatTimer = null

        reconnectTimer?.cancel()
        reconnectTimer = null

        socket?.close(1000, "Normal closure")
        socket = null

        responseCallbacks.clear()

        isConnected = false
        joinedChannels.clear()
        responseCallbacks.clear()
    }

    /**
     * 加入指定频道
     * @param channelName 频道名称，例如 "user:user-uuid"
     * @param authToken 身份验证令牌（可选）
     * @return 加入结果
     */
    suspend fun joinChannel(channelName: String, authToken: String? = null): JSONObject {
        if (joinedChannels.containsKey(channelName)) {
            Log.w(TAG, "Already joined channel: $channelName")
            return JSONObject()
        }

        val ref = generateRef()
        val payload = JSONObject()

        // Add authentication token if provided
        if (authToken != null) {
            Log.d(TAG, "Adding auth token to channel join")
            payload.put("token", authToken)
        } else {
            throw WebSocketException("Auth token is required to join channel")
        }

        val joinPayload = JSONObject().apply {
            put("topic", channelName)
            put("event", EVENT_JOIN)
            put("payload", payload)
            put("ref", ref)
        }
        Log.d(TAG, "Joining channel: $channelName with payload: $joinPayload")

        val response = sendAndWaitForResponse(joinPayload, ref)
        Log.d(TAG, "Joined channel result: $response")
        joinedChannels[channelName] = ref
        return response
    }

    /**
     * 离开指定频道
     * @param channelName 频道名称
     * @return 离开结果
     */
    suspend fun leaveChannel(channelName: String): JSONObject {
        if (!joinedChannels.containsKey(channelName)) {
            throw WebSocketException("Not joined to channel")
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
        return response
    }

    /**
     * 向指定频道发送消息并等待响应
     * @param channelName 频道名称
     * @param event 事件名称
     * @param payload 消息内容
     * @return 服务器响应
     */
    suspend fun push(channelName: String, event: String, payload: JSONObject): JSONObject {
        if (!joinedChannels.containsKey(channelName)) {
            Log.e(TAG, "Cannot send message: Not joined to channel $channelName")
            throw WebSocketException("Not joined to channel")
        }

        val ref = generateRef()
        val messagePayload = JSONObject().apply {
            put("topic", channelName)
            put("event", event)
            put("payload", payload)
            put("ref", ref)
        }

        return sendAndWaitForResponse(messagePayload, ref)
    }

    /**
     * 发送私信
     * @param channelName 频道名称
     * @param receiverId 接收者ID
     * @param text 消息文本内容
     * @param itemId 相关商品ID（可选）
     * @param title 消息标题（可选）
     * @param deepLink 深度链接（可选）
     * @return 发送结果
     */
    suspend fun sendPrivateMessage(
        channelName: String,
        receiverId: String,
        text: String,
        itemId: String? = null,
        title: String? = null,
        deepLink: String? = null
    ): JSONObject {
        val content = JSONObject().apply {
            put("text", text)
            if (itemId != null) put("item_id", itemId)
            if (title != null) put("title", title)
            if (deepLink != null) put("deep_link", deepLink)
        }

        val payload = JSONObject().apply {
            put("receiver_id", receiverId)
            put("content", content)
            put("content_type", CONTENT_TYPE_TEXT)
        }

        return push(channelName, EVENT_SEND_PRIVATE_MESSAGE, payload)
    }

    /**
     * 标记单条消息为已读
     * @param channelName 频道名称
     * @param messageId 消息ID
     * @return 标记结果
     */
    suspend fun ackMessage(channelName: String, messageId: String): JSONObject {
        val payload = JSONObject().apply {
            put("message_id", messageId)
        }

        return push(channelName, EVENT_ACK_MESSAGE, payload)
    }

    /**
     * 批量标记消息为已读
     * @param channelName 频道名称
     * @param messageIds 消息ID列表
     * @return 标记结果
     */
    suspend fun ackMessages(channelName: String, messageIds: List<String>): JSONObject {
        val messageIdsArray = JSONArray()
        messageIds.forEach { messageIdsArray.put(it) }

        val payload = JSONObject().apply {
            put("message_ids", messageIdsArray)
        }

        return push(channelName, EVENT_ACK_MESSAGES, payload)
    }

    /**
     * 获取历史消息
     * @param channelName 频道名称
     * @param before 获取此时间之前的消息（可选）
     * @param limit 消息数量限制（可选）
     * @param messageType 消息类型过滤（可选）
     * @return 历史消息
     */
    suspend fun getHistory(
        channelName: String, before: String? = null, limit: Int? = null, messageType: String? = null
    ): JSONObject {
        val payload = JSONObject()
        if (before != null) payload.put("before", before)
        if (limit != null) payload.put("limit", limit)
        if (messageType != null) payload.put("message_type", messageType)

        return push(channelName, EVENT_GET_HISTORY, payload)
    }

    /**
     * 同步离线消息
     * @param channelName 频道名称
     * @param since 上次同步时间
     * @return 同步结果
     */
    suspend fun syncMessages(channelName: String, since: String): JSONObject {
        val payload = JSONObject().apply {
            put("since", since)
        }

        return push(channelName, EVENT_SYNC_MESSAGES, payload)
    }

    /**
     * 发送消息并等待响应，带超时和重试机制
     */
    private suspend fun sendAndWaitForResponse(payload: JSONObject, ref: String): JSONObject {
        if (!isConnected) {
            throw WebSocketException("WebSocket is not connected")
        }
        return withTimeout(OPERATION_TIMEOUT * 1000) {
            suspendCancellableCoroutine { continuation ->
                responseCallbacks[ref] = CompletableCallback(continuation)

                try {
                    Log.d(TAG, "Sending payload: $payload")
                    socket?.send(payload.toString()) ?: run {
                        responseCallbacks.remove(ref)
                        throw WebSocketException("WebSocket is null")
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
    }

    private val eventListeners =
        mutableMapOf<String, MutableMap<String, MutableList<PhoenixEventCallback>>>()

    /**
     * 监听指定事件
     * @param channelName 频道名称
     * @param event 事件名称
     * @param callback 事件监听器
     */
    fun on(
        channelName: String, event: String, callback: PhoenixEventCallback
    ): PhoenixEventCallback {
        Log.d(TAG, "尝试注册事件回调: $channelName, $event")
        // Create compound key for the channel and event
        val channelCallbacks = eventListeners.getOrPut(channelName) { mutableMapOf() }
        val eventCallbacks = channelCallbacks.getOrPut(event) { mutableListOf() }
        eventCallbacks.add(callback)

        // Modify handleWebSocketMessage to dispatch events to listeners
        CoroutineScope(Dispatchers.IO).launch {
            messageFlow.collect { message ->
                Log.d(TAG, "尝试执行事件回调, $channelName ?= ${message.topic}, $event ?= ${message.event}")
                if (message.topic == channelName && message.event == event) {
                    Log.d(TAG, "事件回调: $message")
                    callback(message)
                }
            }
        }

        return callback
    }

    /**
     * 生成唯一的消息引用号
     */
    private fun generateRef(): String {
        return (++refCounter).toString()
    }

    /**
     * 启动心跳定时器
     */
    private fun startHeartbeat() {
        heartbeatTimer?.cancel()

        heartbeatTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    val ref = generateRef()
                    val heartbeatPayload = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", EVENT_HEARTBEAT)
                        put("payload", JSONObject())
                        put("ref", ref)
                    }
                    socket?.send(heartbeatPayload.toString())
                }
            }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL)
        }
    }

    /**
     * 创建WebSocket监听器
     */
    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection opened")
                isConnected = true
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d(TAG, "Received message: $text")
                    val message = JSONObject(text)
                    handleWebSocketMessage(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code, $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code, $reason")
                handleWebSocketClosure()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleWebSocketFailure(t)
            }
        }
    }

    /**
     * 处理收到的WebSocket消息
     */
    private fun handleWebSocketMessage(message: JSONObject) {
        val topic = message.getString("topic")
        val event = message.getString("event")
        val payload = message.getJSONObject("payload")
        val ref = if (message.has("ref")) message.getString("ref") else null
        val status = if (payload.has("status")) payload.getString("status") else null

        val phoenixMessage = PhoenixMessage(
            topic = topic, event = event, payload = payload, ref = ref, status = status
        )

        // 处理响应回调
        if (ref != null && responseCallbacks.containsKey(ref)) {
            val callback = responseCallbacks[ref]
            responseCallbacks.remove(ref)

            Log.d(TAG, "Response for ref $ref: $payload")

            if (!status.isNullOrEmpty() && status == "error") {
                callback?.continuation?.resumeWithException(
                    WebSocketException(payload.toString())
                )
            } else {
                callback?.continuation?.resume(payload)
            }
        }

        // 发布消息到Flow
        CoroutineScope(Dispatchers.IO).launch {
            _messageFlow.emit(phoenixMessage)
        }
    }

    /**
     * 处理WebSocket关闭
     */
    private fun handleWebSocketClosure() {
        isConnected = false
        heartbeatTimer?.cancel()
        heartbeatTimer = null
    }

    /**
     * 处理WebSocket失败
     */
    private fun handleWebSocketFailure(t: Throwable) {
        Log.e(TAG, "WebSocket failure", t)
        isConnected = false
        heartbeatTimer?.cancel()
        heartbeatTimer = null

        // Emit error to flow
        CoroutineScope(Dispatchers.IO).launch {
            _errorFlow.emit("连接失败: ${t.localizedMessage ?: "未知错误"}")
        }
    }

    /**
     * Phoenix消息结构
     */
    data class PhoenixMessage(
        val topic: String,
        val event: String,
        val payload: JSONObject,
        val ref: String? = null,
        val status: String? = null
    )

    /**
     * WebSocket异常类
     */
    class WebSocketException(message: String) : Exception(message)
}

/**
 * Phoenix事件监听接口
 */
typealias PhoenixEventCallback = (PhoenixMessage) -> Unit

