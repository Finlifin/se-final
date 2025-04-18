package fin.phoenix.flix.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fin.phoenix.flix.data.MessageContentItem
import java.util.Date

@Entity(tableName = "messages")
@TypeConverters(MessageConverters::class, DateConverters::class)
data class MessageEntity(
    @PrimaryKey val id: String,
    val clientMessageId: String?,
    val senderId: String?,
    val receiverId: String?,
    val conversationId: String,
    val content: List<MessageContentItem>,
    val messageType: String,
    val status: String,
    val referenceId: String?,
    val clientTimestamp: Date?,
    val serverTimestamp: Date?,
    val insertedAt: Date,
    val updatedAt: Date,
    val isSending: Boolean = false,
    val sendAttempts: Int = 0,
    val errorMessage: String? = null
)

/**
 * 通用日期转换器，用于Room数据库中Date类型的转换
 */
class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

/**
 * 消息内容转换器
 */
class MessageConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromContentItemList(value: String): List<MessageContentItem> {
        val listType = object : TypeToken<List<MessageContentItem>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toContentItemList(list: List<MessageContentItem>): String {
        return gson.toJson(list)
    }
}