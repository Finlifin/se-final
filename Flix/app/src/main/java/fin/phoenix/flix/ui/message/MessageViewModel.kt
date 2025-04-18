package fin.phoenix.flix.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.api.MessageStatusChange
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.ConversationDetail
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.repository.MessageRepository
import fin.phoenix.flix.repository.ConversationRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val conversationRepository = ConversationRepository(application)
    private val messageRepository = MessageRepository(application)
    private val messageClient = PhoenixMessageClient.instance

    private val currentUserId = application.applicationContext.getSharedPreferences("flix_prefs", 0)
        .getString("user_id", null) ?: ""

    // 会话列表状态
    private val _conversationListState =
        MutableStateFlow<ConversationListState>(ConversationListState.Loading)
    val conversationListState: StateFlow<ConversationListState> = _conversationListState

    // 聊天状态
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Loading)
    val chatState: StateFlow<ChatState> = _chatState

    // 连接状态
    private val _connectionState = messageClient.connectionState
    val connectionState = _connectionState

    // 未读消息计数
    val unreadCounts = messageClient.unreadCounts

    init {
        // 加载会话列表
        loadConversations()

        // 监听新消息
        viewModelScope.launch {
            messageClient.newMessages.collect { message ->
                handleNewMessage(message)
            }
        }

        // 监听消息状态变更
        viewModelScope.launch {
            messageClient.messageStatusChanges.collect { statusChange ->
                handleMessageStatusChange(statusChange)
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                _conversationListState.value = ConversationListState.Loading
                messageRepository.getAllConversations().collect { conversations ->
                    _conversationListState.value = ConversationListState.Success(conversations)
                }
            } catch (e: Exception) {
                _conversationListState.value =
                    ConversationListState.Error(e.message ?: "加载会话列表失败")
            }
        }
    }

    fun loadChat(conversationId: String) {
        viewModelScope.launch {
            try {
                _chatState.value = ChatState.Loading
                messageRepository.getMessagesForConversation(conversationId).collect { messages ->
                    _chatState.value = ChatState.Success(messages)
                }
            } catch (e: Exception) {
                _chatState.value = ChatState.Error(e.message ?: "加载聊天记录失败")
            }
        }
    }

    fun loadSystemMessages(messageType: String) {
        viewModelScope.launch {
            try {
                _chatState.value = ChatState.Loading
                messageRepository.getMessagesForConversation(messageType).collect { messages ->
                    // 根据消息类型过滤和分组
                    val filteredMessages = messages.filter { it.messageType == messageType }
                        .sortedByDescending { it.insertedAt }.groupBy {
                            when {
                                isToday(it.insertedAt) -> "今天"
                                isYesterday(it.insertedAt) -> "昨天"
                                isThisWeek(it.insertedAt) -> "本周"
                                isThisMonth(it.insertedAt) -> "本月"
                                else -> "更早"
                            }
                        }
                    _chatState.value = ChatState.Success(messages)
                }
            } catch (e: Exception) {
                _chatState.value = ChatState.Error(e.message ?: "加载消息失败")
            }
        }
    }

    private fun isToday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        calendar.time = date
        val messageDay = calendar.get(Calendar.DAY_OF_YEAR)
        return today == messageDay && calendar.get(Calendar.YEAR) == Calendar.getInstance()
            .get(Calendar.YEAR)
    }

    private fun isYesterday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.get(Calendar.DAY_OF_YEAR)
        calendar.time = date
        val messageDay = calendar.get(Calendar.DAY_OF_YEAR)
        return yesterday == messageDay && calendar.get(Calendar.YEAR) == Calendar.getInstance()
            .get(Calendar.YEAR)
    }

    private fun isThisWeek(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val thisWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        calendar.time = date
        val messageWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        return thisWeek == messageWeek && calendar.get(Calendar.YEAR) == Calendar.getInstance()
            .get(Calendar.YEAR)
    }

    private fun isThisMonth(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val thisMonth = calendar.get(Calendar.MONTH)
        calendar.time = date
        val messageMonth = calendar.get(Calendar.MONTH)
        return thisMonth == messageMonth && calendar.get(Calendar.YEAR) == Calendar.getInstance()
            .get(Calendar.YEAR)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun sendMessage(
        sender: String,
        conversationId: String,
        content: List<MessageContentItem>,
        messageType: String = MessageTypes.PRIVATE_MESSAGE,
        clientMessageId: String = Uuid.random().toString(),
    ) {
        viewModelScope.launch {
            try {
                val now = Date()
                val message = Message(
                    id = clientMessageId, // Use clientMessageId as temporary id
                    conversationId = conversationId,
                    content = content,
                    messageType = messageType,
                    status = "sending",
                    insertedAt = now,
                    updatedAt = now,
                    clientMessageId = clientMessageId,
                    senderId = currentUserId, // Use the current user's ID
                    receiverId = null, // Will be set by server
                    referenceId = null, // Optional reference
                    clientTimestamp = now,
                    serverTimestamp = now,
                    sender = sender,
                    receiver = null, // Will be set by server
                    isSending = true,
                    sendAttempts = 0,
                    errorMessage = null
                )
                messageRepository.sendMessage(message)
            } catch (e: Exception) {
                _chatState.update { currentState ->
                    if (currentState is ChatState.Success) {
                        ChatState.Success(
                            currentState.messages,
                            error = e.message ?: "发送消息失败"
                        )
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    fun retryMessage(message: Message) {
        viewModelScope.launch {
            messageRepository.retryFailedMessage(message)
        }
    }

    fun withdrawMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.withdrawMessage(messageId)
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            messageRepository.markMessageAsRead(messageId)
        }
    }

    fun markSystemMessageRead(conversationId: String, messageId: String) {
        viewModelScope.launch {
            try {
                messageRepository.markMessageAsRead(messageId)
                // 更新未读计数
                when (conversationId) {
                    "system_notification" -> {
                        messageClient.updateUnreadCounts(MessageTypes.SYSTEM_NOTIFICATION)
                    }

                    "system_announcement" -> {
                        messageClient.updateUnreadCounts(MessageTypes.SYSTEM_ANNOUNCEMENT)
                    }

                    "interaction" -> {
                        messageClient.updateUnreadCounts(MessageTypes.INTERACTION)
                    }
                }
            } catch (e: Exception) {
                // 处理错误
                _chatState.update { currentState ->
                    if (currentState is ChatState.Success) {
                        ChatState.Success(currentState.messages, error = "标记已读失败")
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    fun togglePin(conversationId: String, isPinned: Boolean) {
        viewModelScope.launch {
            messageRepository.toggleConversationPin(conversationId, isPinned)
        }
    }

    fun toggleMute(conversationId: String, isMuted: Boolean) {
        viewModelScope.launch {
            messageRepository.toggleConversationMute(conversationId, isMuted)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.deleteConversation(conversationId)
        }
    }

    private fun handleNewMessage(message: Message) {
        viewModelScope.launch {
            when(val result = conversationRepository.getConversation(message.conversationId)) {
                is Resource.Success -> {
                    val conversationDetail = result.data
                    _conversationListState.update { currentState ->
                        if (currentState is ConversationListState.Success) {
                            val updatedConversations = currentState.conversations.toMutableList()
                            updatedConversations.add(conversationDetail)
                            ConversationListState.Success(updatedConversations)
                        } else {
                            currentState
                        }
                    }
                }
                is Resource.Error -> {
                    // 处理错误
                    _chatState.update { currentState ->
                        if (currentState is ChatState.Success) {
                            ChatState.Success(currentState.messages, error = "获取会话失败")
                        } else {
                            currentState
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun handleMessageStatusChange(statusChange: MessageStatusChange) {
        viewModelScope.launch {
            messageRepository.markMessageAsRead(statusChange.messageId)
        }
    }

    // 状态类
    sealed class ConversationListState {
        object Loading : ConversationListState()
        data class Success(val conversations: List<ConversationDetail>) : ConversationListState()
        data class Error(val message: String) : ConversationListState()
    }

    sealed class ChatState {
        object Loading : ChatState()
        data class Success(
            val messages: List<Message>, val error: String? = null
        ) : ChatState()

        data class Error(val message: String) : ChatState()
    }
}