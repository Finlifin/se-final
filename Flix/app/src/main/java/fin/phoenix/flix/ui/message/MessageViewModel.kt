package fin.phoenix.flix.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.repository.MessageRepository
import kotlinx.coroutines.launch

/**
 * 消息视图模型类
 * 负责管理会话列表和消息中心
 */
class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepository = MessageRepository.getInstance(application)
    private val currentUser = UserManager.getInstance(application)
    private val currentUserId = currentUser.currentUserId.value ?: ""

    // 会话列表状态
    private val _conversationListState = MutableLiveData<ConversationListState>()
    val conversationListState: LiveData<ConversationListState> = _conversationListState

    // WebSocket连接状态
    val connectionState = messageRepository.connectionState


    // 未读消息计数 - 直接使用客户端的未读计数
    val unreadCounts = messageRepository.messageClient.unreadCounts

    init {
        // 加载会话列表
        loadConversations()

        // 监听新消息
        viewModelScope.launch {
            messageRepository.messageClient.newMessages.collect { message ->
                // 更新会话列表状态 - 收到新消息时刷新列表
                updateConversationList()
            }
        }

        viewModelScope.launch {
            conversationListState.observeForever {
                when (it) {
                    is ConversationListState.Success -> {
                        // 从数据库获取每个会话的最后一条消息
                        for (conversation in it.conversations) {
                            if (conversation.lastMessageId == null) continue
                            viewModelScope.launch {
                                val lastMessage =
                                    messageRepository.getMessageById(conversation.lastMessageId)
                                conversation.lastMessage = lastMessage
                            }
                        }
                    }

                    is ConversationListState.Error -> {
                        // 处理错误状态
                    }

                    is ConversationListState.Loading -> {
                        // 处理加载状态
                    }
                }
            }
        }

        // 启动同步
        viewModelScope.launch {
            messageRepository.sync()
        }
    }

    private suspend fun updateConversationList() {
        try {
            // 从数据库获取会话列表，这里会自动包含未读计数
            messageRepository.getAllConversations().observeForever { conversations ->
                _conversationListState.postValue(
                    ConversationListState.Success(
                        conversations
                    )
                )
            }

        } catch (e: Exception) {
            _conversationListState.postValue(
                ConversationListState.Error(e.message ?: "更新会话列表失败")
            )
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

    // 标记整个会话为已读 - 消息中心列表点击会话时使用
    fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch {
            try {
                // 标记会话已读（这里会清空未读数）
                messageRepository.markConversationRead(conversationId)

                // 刷新会话列表以更新UI
                updateConversationList()
            } catch (e: Exception) {
                // 处理错误，可以通过新增的UI状态通知用户
            }
        }
    }

    fun markAllMessagesAsRead() {
        viewModelScope.launch {
            try {
                messageRepository.markAllMessagesRead()
                updateConversationList()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                messageRepository.deleteConversation(conversationId)
                updateConversationList()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    // 状态类
    sealed class ConversationListState {
        object Loading : ConversationListState()
        data class Success(val conversations: List<Conversation>) : ConversationListState()
        data class Error(val message: String) : ConversationListState()
    }
}

