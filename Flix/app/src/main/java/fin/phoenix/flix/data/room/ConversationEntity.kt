package fin.phoenix.flix.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Entity(tableName = "conversations")
@TypeConverters(ConversationConverters::class, DateConverters::class)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val type: String,
    val participantIds: List<String>,
    val lastMessageId: String?,
    val lastMessageContent: String?,
    val lastMessageTimestamp: Date?,
    val updatedAt: Date,
    val insertedAt: Date,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val lastReadMessageId: String? = null
)

class ConversationConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return gson.toJson(list)
    }
}