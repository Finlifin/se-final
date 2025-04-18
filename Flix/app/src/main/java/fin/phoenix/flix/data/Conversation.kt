package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 会话实体类
 */
data class Conversation(
    @SerializedName("id") val id: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("type") val type: String,
    @SerializedName("participant_ids") val participantIds: List<String>,
    @SerializedName("last_message_id") val lastMessageId: String?,
    @SerializedName("last_message_content") val lastMessageContent: String?,
    @SerializedName("last_message_timestamp") val lastMessageTimestamp: Date?,
    @SerializedName("updated_at") val updatedAt: Date,
    @SerializedName("inserted_at") val insertedAt: Date
) {
    fun counterPartyId(currentUserId: String): String? {
        return participantIds.firstOrNull { it != currentUserId }
    }
}

/**
 * 会话设置信息
 */
data class ConversationUserSettings(
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("is_pinned") val isPinned: Boolean,
    @SerializedName("is_muted") val isMuted: Boolean,
    @SerializedName("last_read_message_id") val lastReadMessageId: String?,
    @SerializedName("draft") val draft: String?
)

/**
 * 会话列表项，包含会话和用户设置
 */
data class ConversationListItem(
    @SerializedName("conversation") val conversation: Conversation,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("is_pinned") val isPinned: Boolean,
    @SerializedName("is_muted") val isMuted: Boolean,
    @SerializedName("last_read_message_id") val lastReadMessageId: String?
)

/**
 * 会话详情，包含会话和用户设置
 */
data class ConversationDetail(
    @SerializedName("conversation") val conversation: Conversation,
    @SerializedName("user_settings") val userSettings: ConversationUserSettings
)

/**
 * 创建会话的请求体
 */
data class CreateConversationRequest(
    @SerializedName("type") val type: String,
    @SerializedName("participant_ids") val participantIds: List<String>
)

/**
 * 创建会话的返回结果
 */
data class CreateConversationResponse(
    @SerializedName("conversation") val conversation: Conversation,
    @SerializedName("already_exists") val alreadyExists: Boolean? = null
)

/**
 * 更新会话设置的请求体
 */
data class UpdateConversationSettingsRequest(
    @SerializedName("is_pinned") val isPinned: Boolean? = null,
    @SerializedName("is_muted") val isMuted: Boolean? = null,
    @SerializedName("draft") val draft: String? = null
)

/**
 * 标记会话为已读的请求体
 */
data class MarkConversationReadRequest(
    @SerializedName("last_read_message_id") val lastReadMessageId: String
)
