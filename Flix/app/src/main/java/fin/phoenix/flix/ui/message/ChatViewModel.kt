package fin.phoenix.flix.ui.message

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.repository.MessageRepository
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

/**
 * 聊天视图模型类
 * 负责处理单个聊天会话的业务逻辑
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepository = MessageRepository.getInstance(application)
    private val currentUser = UserManager.getInstance(application)
    private val currentUserId = currentUser.currentUserId.value ?: ""

    // 聊天状态
    private val _chatState = MutableLiveData<ChatState>(ChatState.Loading)
    val chatState: LiveData<ChatState> = _chatState

    // 连接状态 - 直接从Repository获取
    val connectionState = messageRepository.connectionState

    init {
        // 监听新消息
        viewModelScope.launch {
            messageRepository.messageClient.newMessages.collect { message ->
                // 更新当前聊天状态
                updateChatState(message)
            }
        }
    }

    private fun updateChatState(message: Message) {
        val currentState = _chatState.value
        if (currentState is ChatState.Success && currentState.conversation.id == message.conversationId) {
            _chatState.postValue(
                ChatState.Success(
                    messages = listOf(message) + currentState.messages,
                    conversation = currentState.conversation
                )
            )

            // 如果收到新消息且不是自己发送的，自动标记为已读
            if (message.senderId != currentUserId) {
                markMessageAsRead(message.id)
            }
        }
    }

    // 根据会话ID加载聊天记录
    fun loadChat(conversationId: String) {
        viewModelScope.launch {
            try {
                _chatState.postValue(ChatState.Loading)
                val conversation = messageRepository.getConversationById(conversationId)
                messageRepository.getMessagesByConversationId(conversationId)
                    .observeForever { messages ->
                        _chatState.postValue(
                            ChatState.Success(
                                messages.sortedByDescending { it.createdAt }, conversation!!
                            )
                        )

                        // 加载成功后标记会话已读
                        markConversationAsRead(conversationId)
                    }
            } catch (e: Exception) {
                _chatState.postValue(ChatState.Error(e.message ?: "加载聊天记录失败"))
            }
        }
    }

    // 通过参与者ID加载聊天
    fun loadChatByParticipantId(participantId: String) {
        viewModelScope.launch {
            try {
                _chatState.postValue(ChatState.Loading)

                // 处理系统通知的特殊情况
                if (participantId == "server") {
                    val conversationId = "system:notification"
                    val conversation =
                        messageRepository.getOrCreateConversationWithUser(participantId)
                    messageRepository.getMessagesByConversationId(conversationId)
                        .observeForever { messages ->
                            _chatState.postValue(
                                ChatState.Success(
                                    messages.sortedByDescending { it.createdAt }, conversation
                                )
                            )
                            // 标记为已读
                            markConversationAsRead(conversationId)
                        }
                } else {
                    // 常规聊天 - 先获取或创建会话
                    val conversation = messageRepository.getOrCreateConversationWithUser(participantId)

                    // 初始化会话(如果需要创建)
                    initConversationIfNeeded(participantId)

                    // 加载会话消息
                    messageRepository.getMessagesByConversationId(conversation.id)
                        .observeForever { messages ->
                            _chatState.postValue(
                                ChatState.Success(
                                    messages.sortedByDescending { it.createdAt }, conversation
                                )
                            )
                            // 标记为已读
                            markConversationAsRead(conversation.id)
                        }
                }
            } catch (e: Exception) {
                _chatState.postValue(ChatState.Error(e.message ?: "加载聊天记录失败"))
            }
        }
    }

    // 初始化会话（如果需要）
    private suspend fun initConversationIfNeeded(participantId: String) {
        // 获取与参与者的会话，如果不存在则创建
        val conversation = messageRepository.getConversationWithUser(participantId)
        if (conversation == null) {
            // 尝试从其他数据源获取用户信息，这里可以调用ProfileRepository
            // 暂时使用简化逻辑
            // TODO: 通过ProfileRepository获取完整的用户资料
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun sendMessage(
        receiverId: String,
        content: List<MessageContentItem>,
        messageType: String = MessageTypes.CHAT,
    ) {
        viewModelScope.launch {
            try {
                // 发送消息到服务器
                messageRepository.sendMessage(receiverId, content, messageType)
            } catch (e: Exception) {
                val currentChatState = _chatState.value
                if (currentChatState is ChatState.Success) {
                    _chatState.postValue(
                        ChatState.Success(
                            currentChatState.messages,
                            currentChatState.conversation,
                            error = e.message ?: "发送消息失败"
                        )
                    )
                }
            }
        }
    }

    // 生成会话ID (确保两个用户之间的会话ID相同，无论谁发起)
    private fun getConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

//    // 重试发送失败的消息
//    fun retryMessage(message: Message) {
//        viewModelScope.launch {
//            try {
//                messageRepository.retryFailedMessage(message)
//            } catch (e: Exception) {
//                val currentChatState = _chatState.value
//                if (currentChatState is ChatState.Success) {
//                    _chatState.postValue(
//                        ChatState.Success(
//                            currentChatState.messages,
//                            currentChatState.conversationId,
//                            error = e.message ?: "重试发送失败"
//                        )
//                    )
//                }
//            }
//        }
//    }

    // 撤回消息
//    fun withdrawMessage(messageId: String) {
//        viewModelScope.launch {
//            try {
//                messageRepository.withdrawMessage(messageId)
//            } catch (e: Exception) {
//                val currentChatState = _chatState.value
//                if (currentChatState is ChatState.Success) {
//                    _chatState.postValue(
//                        ChatState.Success(
//                            currentChatState.messages,
//                            currentChatState.conversationId,
//                            error = e.message ?: "撤回消息失败"
//                        )
//                    )
//                }
//            }
//        }
//    }

    // 标记消息为已读
    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            messageRepository.markMessagesRead(listOf(messageId))
        }
    }

    // 标记整个会话为已读
    fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch {
            messageRepository.markConversationRead(conversationId)
        }
    }

    // 清空会话
//    fun clearConversation(conversationId: String) {
//        viewModelScope.launch {
//            try {
//                messageRepository.clearConversation(conversationId)
//                // 清空后重新加载空的会话
//                loadChat(conversationId)
//            } catch (e: Exception) {
//                val currentChatState = _chatState.value
//                if (currentChatState is ChatState.Success) {
//                    _chatState.postValue(
//                        ChatState.Success(
//                            currentChatState.messages,
//                            currentChatState.conversationId,
//                            error = e.message ?: "清空会话失败"
//                        )
//                    )
//                }
//            }
//        }
//    }

    // 加载系统消息
    fun loadSystemMessages() {
        viewModelScope.launch {
            try {
                _chatState.postValue(ChatState.Loading)
                val conversation = messageRepository.getOrCreateConversationWithUser("server")
                messageRepository.getMessagesByConversationId(conversation.id)
                    .observeForever { messages ->
                        // 根据消息创建时间排序
                        val sortedMessages = messages.sortedByDescending { it.createdAt }
                        _chatState.postValue(ChatState.Success(sortedMessages, conversation))
                        // 标记会话已读
                        markConversationAsRead(conversation.id)
                    }
            } catch (e: Exception) {
                _chatState.postValue(ChatState.Error(e.message ?: "加载系统消息失败"))
            }
        }
    }

    // 状态类
    sealed class ChatState {
        object Loading : ChatState()
        data class Success(
            val messages: List<Message>, val conversation: Conversation, val error: String? = null
        ) : ChatState()

        data class Error(val message: String) : ChatState()
    }
}