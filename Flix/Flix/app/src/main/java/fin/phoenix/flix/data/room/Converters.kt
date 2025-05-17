package fin.phoenix.flix.data.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.util.GsonConfig
import org.json.JSONObject
import java.util.Date
import java.util.TimeZone

/**
 * Room数据库类型转换器
 * 用于处理复杂类型的存储和读取
 */
class Converters {
    private val gson = GsonConfig.createPrettyGson()
    private val utcTimeZone = TimeZone.getTimeZone("UTC")

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        // 直接以毫秒值创建Date对象，不进行时区转换
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        // 直接获取毫秒值，不进行时区转换
        return date?.time
    }

    @TypeConverter
    fun fromMessageContentItemList(value: List<MessageContentItem>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMessageContentItemList(value: String?): List<MessageContentItem>? {
        if (value == null) return null
        val type = object : TypeToken<List<MessageContentItem>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromUserAbstract(user: UserAbstract?): String? {
        if (user == null) return null
        return gson.toJson(user)
    }

    @TypeConverter
    fun toUserAbstract(value: String?): UserAbstract? {
        if (value == null) return null
        return gson.fromJson(value, UserAbstract::class.java)
    }
    
    @TypeConverter
    fun fromJSONObject(jsonObject: JSONObject?): String? {
        return jsonObject?.toString()
    }
    
    @TypeConverter
    fun toJSONObject(value: String?): JSONObject? {
        if (value == null) return null
        return JSONObject(value)
    }
}