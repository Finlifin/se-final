package fin.phoenix.flix.ui.message

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessagePreview
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.TextMessageContent
import fin.phoenix.flix.data.UnreadCounts
import fin.phoenix.flix.repository.MessageRepository
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.GsonConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 消息系统ViewModel
 */
class MessageViewModel(
    val context: Context,
) : ViewModel() {
    private val TAG = "MessageViewModel"

    private val repository: MessageRepository =
        MessageRepository(context, PhoenixMessageClient.instance)

    private val gson = GsonConfig.createPrettyGson()

    private val sharedPrefs = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
    private val channelName = "user:" + sharedPrefs.getString("user_id", "")
    private val token = sharedPrefs.getString("auth_token", null)

    // 会话列表状态
    private val _conversationsState =
        MutableStateFlow<Resource<List<MessagePreview>>>(Resource.Loading)
    val conversationsState: StateFlow<Resource<List<MessagePreview>>> = _conversationsState

    // 当前聊天消息状态
    private val _messagesState = MutableStateFlow<Resource<List<Message>>>(Resource.Loading)
    val messagesState: StateFlow<Resource<List<Message>>> = _messagesState

    // 未读消息计数
    private val _unreadCounts = MutableLiveData<UnreadCounts>()
    val unreadCounts: LiveData<UnreadCounts> = _unreadCounts

    // 当前聊天对象ID
    private val _currentChatUserId = MutableStateFlow<String?>(null)
    val currentChatUserId: StateFlow<String?> = _currentChatUserId

    // 消息发送状态
    private val _sendMessageState = MutableStateFlow<Resource<Message>?>(null)
    val sendMessageState: StateFlow<Resource<Message>?> = _sendMessageState

    // 筛选条件
    private val _selectedMessageType = MutableStateFlow(MessageTypes.PRIVATE_MESSAGE)
    val selectedMessageType = _selectedMessageType.asStateFlow()

    // WebSocket 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // 自动重连
    private var autoReconnectJob: kotlinx.coroutines.Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 1000L // 1秒基础延迟
    private val maxReconnectDelay = 30000L // 最大30秒延迟


    init {
        // Initialize event callbacks when ViewModel is created
        setupEventCallbacks()
    }

    private fun setupEventCallbacks() {
        val userId = sharedPrefs.getString("user_id", "") ?: ""

        // Listen for new messages
        PhoenixMessageClient.instance.on(
            channelName, PhoenixMessageClient.EVENT_NEW_MESSAGE
        ) { msg ->
            Log.d(TAG, "收到新消息: ${msg.payload}")
            // Process on the background thread, but ensure UI updates happen on the main thread
            viewModelScope.launch {
                try {
                    val message = gson.fromJson(msg.payload.toString(), Message::class.java)
                    // Update the conversation list with the new message
                    val currentConversations = when (val current = _conversationsState.value) {
                        is Resource.Success -> current.data.toMutableList()
                        else -> mutableListOf()
                    }

                    // Create preview text from message content
                    val previewText = when (val content = message.content) {
                        is TextMessageContent -> content.text
                        else -> "新消息"
                    }

                    // Determine conversation partner ID
                    val userId = sharedPrefs.getString("user_id", "") ?: ""
                    val conversationPartnerId =
                        if (message.senderId == userId) message.receiverId else message.senderId
                            ?: ""

                    // Find existing conversation or create new one
                    val existingIndex = currentConversations.indexOfFirst {
                        it.conversationId == conversationPartnerId || it.user.uid == conversationPartnerId
                    }

                    if (existingIndex >= 0) {
                        // Update existing conversation
                        val existing = currentConversations[existingIndex]
                        currentConversations[existingIndex] = existing.copy(
                            lastMessage = previewText,
                            timestamp = message.insertedAt,
                            unreadCount = if (message.senderId != userId) existing.unreadCount + 1 else existing.unreadCount,
                            lastSenderId = message.senderId ?: ""
                        )
                    } else if (message.sender != null) {
                        // Create new conversation
                        currentConversations.add(
                            0, MessagePreview(
                                id = message.id,
                                conversationId = conversationPartnerId,
                                lastMessage = previewText,
                                unreadCount = if (message.senderId != userId) 1 else 0,
                                timestamp = message.insertedAt,
                                user = message.sender!!,
                                messageType = message.messageType,
                                lastSenderId = message.senderId ?: ""
                            )
                        )
                    }

                    // Sort conversations by timestamp (newest first)
                    currentConversations.sortByDescending { it.timestamp }
                    // Emit new value - this triggers UI update
                    _conversationsState.emit(Resource.Success(currentConversations))

                    // Update current chat messages if needed
                    if (message.senderId == _currentChatUserId.value || message.receiverId == _currentChatUserId.value) {
                        val currentMessages = when (val current = _messagesState.value) {
                            is Resource.Success -> current.data.toMutableList()
                            else -> mutableListOf()
                        }
                        currentMessages.add(message)
                        _messagesState.emit(Resource.Success(currentMessages))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理新消息失败", e)
                }
            }

//            // Update current chat if this is for the active conversation
//            _currentChatUserId.value?.let { currentChatId ->
//                // Check if message belongs to current chat
//                val senderId = payload.optString("sender_id", "")
//                if (senderId == currentChatId || (payload.optString("receiver_id") == currentChatId && senderId == userId)) {
//                    // Refresh messages
//                    viewModelScope.launch {
//                        _messagesState.value = repository.loadChatHistory(currentChatId)
//                    }
//                }
//            }
//
//            // Refresh conversation list if it's a private message
//            if (payload.optString("message_type") == MessageTypes.PRIVATE_MESSAGE) {
//                viewModelScope.launch {
//                    _conversationsState.value = repository.loadRecentConversations(_selectedMessageType.value)
//                }
//            }
        }

        // Listen for message status changes
//        PhoenixMessageClient.instance.on(
//            channelName, PhoenixMessageClient.EVENT_MESSAGE_STATUS_CHANGED
//        ) { message ->
//            Log.d(TAG, "消息状态变更: ${message.payload}")
//            // Refresh current chat if needed
//            _currentChatUserId.value?.let { currentChatId ->
//                viewModelScope.launch {
//                    _messagesState.value = repository.loadChatHistory(currentChatId)
//                }
//            }
//        }

        // Listen for unread count updates
//        PhoenixMessageClient.instance.on(channelName, PhoenixMessageClient.EVENT_UNREAD_COUNT_UPDATE) { message ->
//            Log.d(TAG, "未读计数更新: ${message.payload}")
//            // Update unread counts
//            try {
//                val data = message.payload.getJSONObject("counts")
//                val counts = UnreadCounts(
//                    privateMessages = data.optInt("private_message", 0),
//                    systemNotifications = data.optInt("system_notification", 0),
//                    systemAnnouncements = data.optInt("system_announcement", 0),
//                    interactions = data.optInt("interaction", 0)
//                )
//                _unreadCounts.postValue(counts)
//            } catch (e: Exception) {
//                Log.e(TAG, "解析未读计数失败", e)
//            }
//        }

        // Handle successful message sending
//        PhoenixMessageClient.instance.on(channelName, PhoenixMessageClient.EVENT_SEND_PRIVATE_MESSAGE) { message ->
//            if (message.status == "ok") {
//                val payload = message.payload
//                val receiverId = payload.optString("receiver_id", "")
//                val text = payload.optJSONObject("content")?.optString("text", "") ?: ""
//                val itemId = payload.optJSONObject("content")?.optString("item_id")
//                val title = payload.optJSONObject("content")?.optString("title")
//
//                handleMessageSendSuccess(receiverId, text, itemId, title)
//
//                // Refresh messages for the current chat
//                _currentChatUserId.value?.let { currentChatId ->
//                    if (currentChatId == receiverId) {
//                        viewModelScope.launch {
//                            _messagesState.value = repository.loadChatHistory(currentChatId)
//                        }
//                    }
//                }
//            }
//        }
    }

    /**
     * 发送消息
     */
    fun sendMessage(
        receiverId: String, text: String, itemId: String? = null, title: String? = null
    ) {
        if (text.isBlank() || receiverId.isBlank()) {
            _sendMessageState.value = Resource.Error("无效的消息内容或接收者")
            return
        }

        _sendMessageState.value = Resource.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "发送消息, channelName: $channelName")
                PhoenixMessageClient.instance.sendPrivateMessage(
                    channelName, receiverId, text, itemId, title
                )
            } catch (e: Exception) {
                handleMessageSendFailure(e)
            }
        }
    }

    /**
     * 处理消息发送成功
     */
    private fun handleMessageSendSuccess(
        receiverId: String, text: String, itemId: String?, title: String?
    ) {
        val message = createLocalMessage(receiverId, text, itemId, title)

        _sendMessageState.value = Resource.Success(message)
    }

    /**
     * 处理消息发送失败
     */
    private fun handleMessageSendFailure(error: Throwable) {
        Log.e(TAG, "发送消息失败", error)
        _sendMessageState.value = Resource.Error("发送消息失败：${error.message}")
    }

    /**
     * 创建本地消息对象（用于UI更新）
     */
    private fun createLocalMessage(
        receiverId: String, text: String, itemId: String? = null, title: String? = null
    ): Message {
        val sharedPref = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getString("user_id", "") ?: ""

        return Message(
            id = "local-${System.currentTimeMillis()}",
            senderId = currentUserId,
            receiverId = receiverId,
            content = TextMessageContent(
                text = text, itemId = itemId, title = title
            ),
            contentType = "text",
            messageType = MessageTypes.PRIVATE_MESSAGE,
            status = "sent",
            insertedAt = Date(),
            updatedAt = Date()
        )
    }

//    /**
//     * 标记消息为已读
//     */
//    fun markMessageAsRead(messageId: String) {
//        viewModelScope.launch {
//            try {
//                messageService?.markMessageAsRead(messageId) ?: repository.markMessageAsRead(
//                    messageId
//                )
//            } catch (e: Exception) {
//                Log.e(TAG, "标记消息已读失败", e)
//            }
//        }
//    }
//
//    /**
//     * 批量标记消息为已读
//     */
//    fun markMessagesAsRead(messageIds: List<String>) {
//        if (messageIds.isEmpty()) return
//
//        viewModelScope.launch {
//            try {
//                messageService?.markMessagesAsRead(messageIds) ?: repository.markMessagesAsRead(
//                    messageIds
//                )
//            } catch (e: Exception) {
//                Log.e(TAG, "批量标记消息已读失败", e)
//            }
//        }
//    }
//
//    /**
//     * 加载未读消息计数
//     */
//    private fun loadUnreadCounts() {
//        viewModelScope.launch {
//            try {
//                val result = repository.fetchUnreadCounts()
//                if (result is Resource.Success) {
//                    _unreadCounts.postValue(result.data)
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "加载未读消息计数失败", e)
//            }
//        }
//    }

    /**
     * 更改消息筛选类型
     */
    fun setMessageType(messageType: String) {
        if (_selectedMessageType.value != messageType) {
            _selectedMessageType.value = messageType
            viewModelScope.launch {
                Log.d(TAG, "筛选消息类型: $messageType")
                _conversationsState.value = repository.loadRecentConversations(messageType)
            }
        }
    }

    /**
     * 清除当前聊天状态
     */
    fun clearCurrentChat() {
        _currentChatUserId.value = null
        _messagesState.value = Resource.Loading
    }

    /**
     * 清除消息发送状态
     */
    fun clearSendMessageState() {
        _sendMessageState.value = null
    }

    fun loadMessages(partnerUserId: String) {
        _currentChatUserId.value = partnerUserId
        _messagesState.value = Resource.Loading

        viewModelScope.launch {
            try {
                _messagesState.value = repository.loadChatHistory(partnerUserId)
            } catch (e: Exception) {
                Log.e(TAG, "加载消息失败", e)
                _messagesState.value = Resource.Error("加载消息失败: ${e.message}")
            }
        }
    }

    /**
     * WebSocket 连接状态
     */
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data class Failed(val reason: String) : ConnectionState()
    }
}

/**
 * MessageViewModel工厂类
 */
class MessageViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MessageViewModel(
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

