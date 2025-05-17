package fin.phoenix.flix.data.room

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.data.repository.getConversationId
import java.util.Date

/**
 * 消息数据库实体类
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val clientMessageId: String?,
    val senderId: String,
    val receiverId: String,
    val conversationId: String,
    val content: List<MessageContentItem>,
    val messageType: String,
    val sender: UserAbstract?,
    val receiver: UserAbstract?,
    val status: String?,
    val createdAt: Date,
    val updatedAt: Date,
    val isSending: Boolean,
    val sendAttempts: Int,
    val errorMessage: String?
) {
    /**
     * 将实体转换为领域模型
     */
    fun toDomainModel(): Message {
        return Message(
            id = id,
            clientMessageId = clientMessageId,
            senderId = senderId,
            receiverId = receiverId,
            conversationId = conversationId,
            content = content,
            messageType = messageType,
            sender = sender,
            receiver = receiver,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isSending = isSending,
            sendAttempts = sendAttempts,
            errorMessage = errorMessage
        )
    }

    companion object {
        /**
         * 从领域模型创建数据库实体
         */
        fun fromDomainModel(message: Message): MessageEntity {
            return MessageEntity(
                id = message.id,
                clientMessageId = message.clientMessageId,
                senderId = message.senderId,
                receiverId = message.receiverId,
                conversationId = message.conversationId ?: getConversationId(message.senderId, message.receiverId),
                content = message.content,
                messageType = message.messageType,
                sender = message.sender,
                receiver = message.receiver,
                status = message.status,
                createdAt = message.createdAt,
                updatedAt = message.updatedAt,
                isSending = message.isSending,
                sendAttempts = message.sendAttempts,
                errorMessage = message.errorMessage
            )
        }
    }
}