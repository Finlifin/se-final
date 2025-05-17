package fin.phoenix.flix.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.UserAbstract

/**
 * 会话数据库实体类
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    var participantId: String,
    var lastMessageId: String?,
    val participant: UserAbstract?,
    var unreadCounts: Int
) {
    /**
     * 将实体转换为领域模型
     */
    fun toDomainModel(): Conversation {
        return Conversation(
            id = id,
            participantId = participantId,
            lastMessageId = lastMessageId,
            participant = participant,
            unreadCounts = unreadCounts,
            lastMessage = null // 通过DAO关联查询填充
        )
    }

    companion object {
        /**
         * 从领域模型创建数据库实体
         */
        fun fromDomainModel(conversation: Conversation): ConversationEntity {
            return ConversationEntity(
                id = conversation.id,
                participantId = conversation.participantId,
                lastMessageId = conversation.lastMessageId,
                participant = conversation.participant,
                unreadCounts = conversation.unreadCounts
            )
        }
    }
}