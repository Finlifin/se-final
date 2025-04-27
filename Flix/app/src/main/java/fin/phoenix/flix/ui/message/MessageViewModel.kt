package fin.phoenix.flix.ui.message

import android.app.Application
import android.icu.util.Calendar
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.ConversationDetail
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.MessageStatus
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.data.repository.MessageRepository
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepository = MessageRepository.getInstance(application)
    private val currentUser = UserManager.getInstance(application)
    private val currentUserId = currentUser.currentUserId.value ?: ""

    // 会话列表状态
    private val _conversationListState =
        MutableLiveData<ConversationListState>(ConversationListState.Loading)
    val conversationListState: LiveData<ConversationListState> = _conversationListState

    // 聊天状态
    private val _chatState = MutableLiveData<ChatState>(ChatState.Loading)
    val chatState: LiveData<ChatState> = _chatState

    // 连接状态 - 直接从Repository获取
    val connectionState = messageRepository.connectionState

    // 未读消息计数 - 直接从Repository获取
    val unreadCounts = messageRepository.unreadCounts

    init {
        // 加载会话列表
        loadConversations()

        // 监听新消息
        viewModelScope.launch {
            messageRepository.newMessages.collect { message ->
                // 更新当前聊天状态
                updateChatState(message)
                // 更新会话列表状态
                updateConversationList()
            }
        }

        // 启动同步
        viewModelScope.launch {
            messageRepository.sync()
        }
    }

    private suspend fun updateChatState(message: Message) {
        val currentState = _chatState.value
        if (currentState is ChatState.Success && currentState.conversationId == message.conversationId) {
            _chatState.postValue(
                ChatState.Success(
                    messages = listOf(message) + currentState.messages,
                    conversationId = currentState.conversationId
                )
            )
            
            // 如果收到新消息且不是自己发送的，自动标记为已读
            if (message.senderId != currentUserId) {
                markMessageAsRead(message.id)
            }
        }
    }

    private suspend fun updateConversationList() {
        messageRepository.getAllConversations().collect { conversations ->
            _conversationListState.postValue(ConversationListState.Success(conversations))
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                _conversationListState.postValue(ConversationListState.Loading)
                updateConversationList()
            } catch (e: Exception) {
                _conversationListState.postValue(
                    ConversationListState.Error(
                        e.message ?: "加载会话列表失败"
                    )
                )
            }
        }
    }

    fun loadChat(conversationId: String) {
        viewModelScope.launch {
            try {
                _chatState.postValue(ChatState.Loading)
                messageRepository.getMessagesForConversation(conversationId).collect { messages ->
                    _chatState.postValue(ChatState.Success(messages, conversationId))
                    
                    // 加载成功后标记会话已读
                    markConversationAsRead(conversationId)
                }
            } catch (e: Exception) {
                _chatState.postValue(ChatState.Error(e.message ?: "加载聊天记录失败"))
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun sendMessage(
        conversationId: String,
        content: List<MessageContentItem>,
        messageType: String = MessageTypes.PRIVATE_MESSAGE,
        clientMessageId: String = Uuid.random().toString(),
    ) {
        viewModelScope.launch {
            try {
                val now = Date()
                val message = Message(
                    id = clientMessageId,
                    conversationId = conversationId,
                    content = content,
                    messageType = messageType,
                    status = MessageStatus.SENDING,
                    insertedAt = now,
                    updatedAt = now,
                    clientMessageId = clientMessageId,
                    senderId = currentUserId,
                    receiverId = null,
                    referenceId = null,
                    clientTimestamp = now,
                    serverTimestamp = now,
                    sender = currentUser.currentUser.value,
                    receiver = null,
                    isSending = true,
                    sendAttempts = 0,
                    errorMessage = null
                )
                messageRepository.sendMessage(message)
            } catch (e: Exception) {
                val currentChatState = _chatState.value
                if (currentChatState is ChatState.Success) {
                    _chatState.postValue(
                        ChatState.Success(
                            currentChatState.messages,
                            currentChatState.conversationId,
                            error = e.message ?: "发送消息失败"
                        )
                    )
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
    
    // 标记整个会话为已读
    fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch {
            messageRepository.markConversationAsRead(conversationId)
        }
    }

    fun markSystemMessageRead(conversationId: String, messageId: String) {
        viewModelScope.launch {
            try {
                messageRepository.markMessageAsRead(messageId)
            } catch (e: Exception) {
                val currentChatState = _chatState.value
                if (currentChatState is ChatState.Success) {
                    _chatState.postValue(
                        ChatState.Success(
                            currentChatState.messages,
                            currentChatState.conversationId,
                            error = "标记已读失败"
                        )
                    )
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

    fun clearConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.clearConversation(conversationId)
        }
    }

    fun loadSystemMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                _chatState.postValue(ChatState.Loading)
                messageRepository.getMessagesForConversation(conversationId).collect { messages ->
                    // 根据消息类型过滤和分组
                    val filteredMessages = messages.sortedByDescending { it.insertedAt }.groupBy {
                        when {
                            isToday(it.insertedAt) -> "今天"
                            isYesterday(it.insertedAt) -> "昨天"
                            isThisWeek(it.insertedAt) -> "本周"
                            isThisMonth(it.insertedAt) -> "本月"
                            else -> "更早"
                        }
                    }
                    _chatState.postValue(ChatState.Success(messages, conversationId))
                }
            } catch (e: Exception) {
                _chatState.postValue(ChatState.Error(e.message ?: "加载消息失败"))
            }
        }
    }
    
    // 辅助函数，用于判断时间
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

    // 状态类
    sealed class ConversationListState {
        object Loading : ConversationListState()
        data class Success(val conversations: List<ConversationDetail>) : ConversationListState()
        data class Error(val message: String) : ConversationListState()
    }

    sealed class ChatState {
        object Loading : ChatState()
        data class Success(
            val messages: List<Message>, val conversationId: String, val error: String? = null
        ) : ChatState()
        data class Error(val message: String) : ChatState()
    }
}

