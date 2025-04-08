package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * 消息数据模型
 */
data class Message(
    @SerializedName("id") val id: String,
    @SerializedName("sender_id") val senderId: String?,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("content") val content: MessageContent,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("status") val status: String,
    @SerializedName("reference_id") val referenceId: String? = null,
    @SerializedName("inserted_at") val insertedAt: Date,
    @SerializedName("updated_at") val updatedAt: Date,
    // Additional UI fields
    @SerializedName("sender") var sender: UserAbstract? = null,
    @SerializedName("receiver") val receiver: UserAbstract? = null
)

/**
 * 消息内容接口
 */
interface MessageContent

/**
 * 文本消息内容
 */
data class TextMessageContent(
    @SerializedName("text") val text: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("item_id") val itemId: String? = null,
    @SerializedName("deep_link") val deepLink: String? = null,
    @SerializedName("image_urls") val imageUrls: List<String>? = null
) : MessageContent

/**
 * 系统通知消息内容
 */
data class SystemNotificationContent(
    @SerializedName("text") val text: String,
    @SerializedName("title") val title: String,
    @SerializedName("deep_link") val deepLink: String? = null
) : MessageContent

/**
 * 系统公告消息内容
 */
data class SystemAnnouncementContent(
    @SerializedName("text") val text: String,
    @SerializedName("title") val title: String,
    @SerializedName("deep_link") val deepLink: String? = null
) : MessageContent

/**
 * 商品互动消息内容
 */
data class InteractionMessageContent(
    @SerializedName("text") val text: String,
    @SerializedName("title") val title: String,
    @SerializedName("item_id") val itemId: String,
    @SerializedName("interaction_type") val interactionType: String,
    @SerializedName("deep_link") val deepLink: String? = null,
    @SerializedName("comment_id") val commentId: String? = null
) : MessageContent

/**
 * 消息摘要，用于会话列表
 */
data class MessagePreview(
    val id: String,
    val conversationId: String,
    val lastMessage: String,
    val unreadCount: Int,
    val timestamp: Date,
    val user: UserAbstract,
    val messageType: String,
    val lastSenderId: String
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
    const val UNREAD = "unread"
    const val READ = "read"
    const val DELETED = "deleted"
}

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
}