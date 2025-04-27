package fin.phoenix.flix.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import fin.phoenix.flix.data.InteractionPayload
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.Order
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.SystemAnnouncementPayload
import fin.phoenix.flix.data.SystemNotificationPayload
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility class for Gson configuration
 * Provides properly configured Gson instances for the application
 */
object GsonConfig {
    
    /**
     * Creates a Gson instance with all required type adapters registered
     */
    fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(MessageContentItem::class.java, MessageContentItemDeserializer())
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .create()
    }
    
    /**
     * Creates a Gson instance with pretty printing for debugging purposes
     */
    fun createPrettyGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(MessageContentItem::class.java, MessageContentItemDeserializer())
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .setPrettyPrinting()
            .create()
    }
    
    /**
     * 消息内容项反序列化器，用于处理不同类型的消息内容
     */
    class MessageContentItemDeserializer : JsonDeserializer<MessageContentItem>, JsonSerializer<MessageContentItem> {
        private val gson = Gson()
        
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MessageContentItem {
            val jsonObject = json.asJsonObject
            val type = jsonObject.get("type").asString
            val payloadElement = jsonObject.get("payload")
            
            // 根据不同的类型处理不同的载荷
            val payload: Any = when (type) {
                // 基本类型
                "text" -> payloadElement.asString
                "image" -> payloadElement.asString
                "video" -> payloadElement.asString
                "audio" -> payloadElement.asString
                "comment" -> payloadElement.asString
                "like" -> payloadElement.asString
                
                // 复杂类型 - 使用正确的数据类
                "product" -> context.deserialize<Product>(payloadElement, Product::class.java)
                "order" -> context.deserialize<Order>(payloadElement, Order::class.java)
                "system" -> context.deserialize<SystemNotificationPayload>(payloadElement, SystemNotificationPayload::class.java)
                "notification" -> context.deserialize<SystemNotificationPayload>(payloadElement, SystemNotificationPayload::class.java)
                "announcement" -> context.deserialize<SystemAnnouncementPayload>(payloadElement, SystemAnnouncementPayload::class.java)
                "interaction" -> context.deserialize<InteractionPayload>(payloadElement, InteractionPayload::class.java)
                
                // 收藏信息需要特别处理
                "favorite" -> {
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    context.deserialize<Map<String, Any>>(payloadElement, mapType)
                }
                
                // 对于未知类型，尝试解析为Map或保持为JsonElement
                else -> {
                    if (payloadElement.isJsonObject) {
                        val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                        try {
                            context.deserialize<Map<String, Any>>(payloadElement, mapType)
                        } catch (e: Exception) {
                            payloadElement
                        }
                    } else {
                        payloadElement
                    }
                }
            }
            
            return MessageContentItem(type, payload)
        }

        override fun serialize(src: MessageContentItem, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("type", src.type)
            
            // 根据载荷类型序列化
            val payload = when (src.payload) {
                is String -> JsonPrimitive(src.payload as String)
                is Number -> JsonPrimitive(src.payload as Number)
                is Boolean -> JsonPrimitive(src.payload as Boolean)
                else -> context.serialize(src.payload)
            }
            
            jsonObject.add("payload", payload)
            return jsonObject
        }
    }
/**
     * 增强的日期适配器，支持多种日期格式并使用本地时区
     */
    class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
        private val utcFormats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        )

        private val localFormats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        // 本地时区，用于输出
        private val localTimeZone = TimeZone.getDefault()

        override fun serialize(src: Date, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            // 使用本地时区的ISO格式输出日期
            val localFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            localFormat.timeZone = localTimeZone

            // 添加本地时区偏移
            val offset = localTimeZone.getOffset(src.time) / (60 * 60 * 1000)
            val offsetSign = if (offset >= 0) "+" else "-"
            val offsetString = String.format("%s%02d:00", offsetSign, Math.abs(offset))

            return JsonPrimitive(localFormat.format(src) + offsetString)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date {
            if (json.isJsonPrimitive) {
                val jsonPrimitive = json.asJsonPrimitive

                // 尝试作为长整型时间戳解析
                if (jsonPrimitive.isNumber) {
                    return try {
                        Date(jsonPrimitive.asLong)
                    } catch (e: NumberFormatException) {
                        Date()
                    }
                }

                // 尝试作为字符串日期解析
                if (jsonPrimitive.isString) {
                    val dateStr = jsonPrimitive.asString

                    // 如果是UTC格式（以Z结尾），使用UTC格式解析
                    if (dateStr.endsWith("Z")) {
                        for (format in utcFormats) {
                            try {
                                return format.parse(dateStr)
                            } catch (e: Exception) {
                                // 忽略解析异常，尝试下一个格式
                            }
                        }
                    }

                    // 尝试本地时区格式
                    for (format in localFormats) {
                        try {
                            return format.parse(dateStr)
                        } catch (e: Exception) {
                            // 忽略解析异常，尝试下一个格式
                        }
                    }
                }
            }

            // 如果所有尝试都失败，返回当前时间
            return Date()
        }
    }
}
