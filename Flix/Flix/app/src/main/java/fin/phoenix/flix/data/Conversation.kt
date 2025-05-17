package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName

/**
 * 会话实体类
 */
data class Conversation(
    @SerializedName("id") val id: String,
    @SerializedName("participant_id") val participantId: String,
    @SerializedName("last_message_id") val lastMessageId: String?,

    @SerializedName("participant") var participant: UserAbstract?,
    @SerializedName("unread_counts") var unreadCounts: Int,
    @SerializedName("last_message") var lastMessage: Message?,

    ) {
    suspend fun fulfill() {
        // TODO: this method fetches the participant, last message, and unread counts
    }
}
