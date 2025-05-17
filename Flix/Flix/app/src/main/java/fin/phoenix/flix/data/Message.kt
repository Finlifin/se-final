package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.util.Date

/**
 * 消息数据模型
 */
data class Message(
    @SerializedName("id") val id: String,
    @SerializedName("message_id") val clientMessageId: String? = null,
    @SerializedName("sender") val senderId: String,
    @SerializedName("receiver") val receiverId: String,
    @SerializedName("conversation_id") val conversationId: String?,
    @SerializedName("content") val content: List<MessageContentItem>,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("sender_abstract") var sender: UserAbstract? = null,
    @SerializedName("receiver_abstract") val receiver: UserAbstract? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("inserted_at") var createdAt: Date,
    @SerializedName("updated_at") val updatedAt: Date,

    var isSending: Boolean = false,
    var sendAttempts: Int = 0,
    var errorMessage: String? = null
)

/**
 * 消息内容项
 */
data class MessageContentItem(
    @SerializedName("type") val type: String, @SerializedName("payload") val payload: Any
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
 * 消息类型
 */
object MessageTypes {
    const val SYSTEM = "system"
    const val NOTIFICATION = "notification"
    const val CHAT = "chat"
    const val ORDER = "order"
    const val PAYMENT = "payment"
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

/**
 * 同步结果数据
 */
data class SyncResult(
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("new_last_sync_timestamp") val newLastSyncTimestamp: Date
)