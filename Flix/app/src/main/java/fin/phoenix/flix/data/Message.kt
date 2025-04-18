package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 消息数据模型
 */
data class Message(
    @SerializedName("id") val id: String,
    @SerializedName("client_message_id") val clientMessageId: String? = null,
    @SerializedName("sender_id") val senderId: String?,
    @SerializedName("receiver_id") val receiverId: String?,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("content") val content: List<MessageContentItem>,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("status") val status: String,
    @SerializedName("reference_id") val referenceId: String? = null,
    @SerializedName("client_timestamp") val clientTimestamp: Date? = null,
    @SerializedName("server_timestamp") val serverTimestamp: Date? = null,
    @SerializedName("inserted_at") val insertedAt: Date,
    @SerializedName("updated_at") val updatedAt: Date,
    // Additional UI fields
    @SerializedName("sender") var sender: String = "<unknown>",
    @SerializedName("receiver") val receiver: String? = null,
    // Local fields (not from server)
    var isSending: Boolean = false,
    var sendAttempts: Int = 0,
    var errorMessage: String? = null
)

/**
 * 消息内容项
 */
data class MessageContentItem(
    @SerializedName("type") val type: String,
    @SerializedName("payload") val payload: Any
)

/**
 * 消息内容类型
 */
object ContentTypes {
    const val TEXT = "text"
    const val IMAGE = "image"
    const val PRODUCT = "product"
    const val ORDER = "order"
    const val COMMENT = "comment"
    const val LIKE = "like"
    const val FAVORITE = "favorite"
    const val SYSTEM = "system"
    const val AUDIO = "audio"
    const val VIDEO = "video"
}

/**
 * 系统通知负载
 */
data class SystemNotificationPayload(
    @SerializedName("title") val title: String,
    @SerializedName("text") val text: String,
    @SerializedName("type") val type: String,
    @SerializedName("deep_link") val deepLink: String? = null,
    @SerializedName("data") val data: Map<String, Any>? = null
)

/**
 * 系统公告负载
 */
data class SystemAnnouncementPayload(
    @SerializedName("title") val title: String,
    @SerializedName("text") val text: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("deep_link") val deepLink: String? = null,
    @SerializedName("start_time") val startTime: Date? = null,
    @SerializedName("end_time") val endTime: Date? = null
)

/**
 * 互动消息负载
 */
data class InteractionPayload(
    @SerializedName("title") val title: String,
    @SerializedName("text") val text: String,
    @SerializedName("interaction_type") val interactionType: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("deep_link") val deepLink: String? = null,
    @SerializedName("data") val data: Map<String, Any>? = null
)

/**
 * 互动类型
 */
object InteractionTypes {
    const val FOLLOW = "follow"
    const val LIKE = "like"
    const val COMMENT = "comment"
    const val FAVORITE = "favorite"
    const val SHARE = "share"
    const val MENTION = "mention"
}

/**
 * 商品信息负载
 */
data class ProductPayload(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("price") val price: Double,
    @SerializedName("image_url") val imageUrl: String?
)

/**
 * 订单信息负载
 */
data class OrderPayload(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("order_status") val orderStatus: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("image_url") val imageUrl: String?
)

/**
 * 未读消息计数
 */
data class UnreadCounts(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("system_notification") val systemNotification: Int = 0,
    @SerializedName("system_announcement") val systemAnnouncement: Int = 0,
    @SerializedName("interaction") val interaction: Int = 0,
    @SerializedName("private_message") val privateMessage: Int = 0
)

/**
 * 会话类型
 */
object ConversationTypes {
    const val PRIVATE = "private"
    const val SYSTEM_NOTIFICATION = "system_notification"
    const val SYSTEM_ANNOUNCEMENT = "system_announcement"
    const val INTERACTION = "interaction"
}

/**
 * 消息类型
 */
object MessageTypes {
    const val SYSTEM_NOTIFICATION = "system_notification"
    const val SYSTEM_ANNOUNCEMENT = "system_announcement"
    const val INTERACTION = "interaction"
    const val PRIVATE_MESSAGE = "private_message"
}

/**
 * 消息状态
 */
object MessageStatus {
    const val SENDING = "sending" // 客户端状态
    const val SENT = "sent"
    const val UNREAD = "unread"
    const val READ = "read"
    const val WITHDRAWN = "withdrawn"
    const val DELETED = "deleted"
}
//
///**
// * 会话数据模型
// */
//data class Conversation(
//    @SerializedName("id") val id: String,
//    @SerializedName("conversation_id") val conversationId: String,
//    @SerializedName("type") val type: String,
//    @SerializedName("participant_ids") val participantIds: List<String>,
//    @SerializedName("last_message_id") val lastMessageId: String?,
//    @SerializedName("last_message_content") val lastMessageContent: String?,
//    @SerializedName("last_message_timestamp") val lastMessageTimestamp: Date?,
//    @SerializedName("updated_at") val updatedAt: Date,
//    @SerializedName("inserted_at") val insertedAt: Date,
//    // UI related fields
//    @SerializedName("unread_count") val unreadCount: Int = 0,
//    @SerializedName("is_pinned") val isPinned: Boolean = false,
//    @SerializedName("is_muted") val isMuted: Boolean = false,
//    @SerializedName("last_read_message_id") val lastReadMessageId: String? = null,
//    // Participant info (populated after fetching)
//    var counterparty: UserAbstract? = null
//) {
//    /**
//     * 获取会话中的另一方用户ID（非当前用户）
//     */
//    fun getCounterpartyId(currentUserId: String): String? {
//        return participantIds.firstOrNull { it != currentUserId }
//    }
//}

/**
 * 事件数据模型
 */
data class MessageEvent(
    @SerializedName("id") val id: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("payload") val payload: Any,
    @SerializedName("event_timestamp") val eventTimestamp: Date,
    @SerializedName("target_user_id") val targetUserId: String,
    @SerializedName("inserted_at") val insertedAt: Date
)

/**
 * 事件类型常量
 */
object EventTypes {
    const val NEW_MESSAGE = "new_message"
    const val MESSAGE_STATUS_CHANGED = "message_status_changed"
    const val ORDER_UPDATED = "order_updated"
    const val SYSTEM_NOTIFICATION = "system_notification"
}

/**
 * 同步结果数据
 */
data class SyncResult(
    @SerializedName("events") val events: List<MessageEvent>,
    @SerializedName("new_last_sync_timestamp") val newLastSyncTimestamp: Date
)