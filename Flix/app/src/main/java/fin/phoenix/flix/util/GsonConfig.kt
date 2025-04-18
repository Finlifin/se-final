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
import fin.phoenix.flix.data.OrderPayload
import fin.phoenix.flix.data.ProductPayload
import fin.phoenix.flix.data.SystemAnnouncementPayload
import fin.phoenix.flix.data.SystemNotificationPayload
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                "text" -> payloadElement.asString
                "image" -> payloadElement.asString
                "product" -> context.deserialize<ProductPayload>(payloadElement, ProductPayload::class.java)
                "order" -> context.deserialize<OrderPayload>(payloadElement, OrderPayload::class.java)
                "system" -> context.deserialize<SystemNotificationPayload>(payloadElement, SystemNotificationPayload::class.java)
                "system_announcement" -> context.deserialize<SystemAnnouncementPayload>(payloadElement, SystemAnnouncementPayload::class.java)
                "interaction" -> context.deserialize<InteractionPayload>(payloadElement, InteractionPayload::class.java)
                else -> {
                    // 对于未知类型，尝试解析为Map或保持为JsonElement
                    if (payloadElement.isJsonObject) {
                        val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                        context.deserialize<Map<String, Any>>(payloadElement, mapType)
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
     * 增强的日期适配器，支持多种日期格式
     */
    class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
        private val dateFormats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )

        override fun serialize(src: Date, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(dateFormats[0].format(src))
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
                    
                    // 尝试使用不同的日期格式
                    for (format in dateFormats) {
                        try {
                            format.parse(dateStr)?.let { return it }
                        } catch (e: Exception) {
                            // 继续尝试下一种格式
                        }
                    }
                }
            }
            
            // 如果所有解析方式都失败，返回当前日期
            return Date()
        }
    }
}
