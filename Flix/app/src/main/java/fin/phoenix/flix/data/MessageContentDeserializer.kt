package fin.phoenix.flix.data

import com.google.gson.*
import java.lang.reflect.Type

class MessageContentDeserializer : JsonDeserializer<MessageContentItem> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): MessageContentItem {
        val jsonObject = json.asJsonObject
        val contentType = jsonObject.get("type").asString
        val payload = jsonObject.get("payload")

        return when (contentType) {
            // 文本消息
            "text" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 图片消息
            "image" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 视频消息
            "video" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 音频消息
            "audio" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 商品信息 - 使用已有的Product类
            "product" -> {
                val payloadObj = context.deserialize<Product>(
                    payload,
                    Product::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 订单信息 - 使用已有的Order类
            "order" -> {
                val payloadObj = context.deserialize<Order>(
                    payload,
                    Order::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 评论信息
            "comment" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 点赞信息
            "like" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 收藏信息
            "favorite" -> {
                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                val payloadObj = context.deserialize<Map<String, Any>>(
                    payload,
                    mapType
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 系统消息
            "system" -> {
                val payloadObj = context.deserialize<SystemNotificationPayload>(
                    payload,
                    SystemNotificationPayload::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 系统通知（旧名称，为了向后兼容）
            "notification" -> {
                val payloadObj = context.deserialize<SystemNotificationPayload>(
                    payload,
                    SystemNotificationPayload::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 系统公告
            "announcement" -> {
                val payloadObj = context.deserialize<SystemAnnouncementPayload>(
                    payload,
                    SystemAnnouncementPayload::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 互动消息
            "interaction" -> {
                val payloadObj = context.deserialize<InteractionPayload>(
                    payload,
                    InteractionPayload::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 未知类型,保持原始JSON
            else -> {
                // 尝试解析为Map或保持原始字符串
                if (payload.isJsonObject) {
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    try {
                        val payloadObj = context.deserialize<Map<String, Any>>(payload, mapType)
                        MessageContentItem(type = contentType, payload = payloadObj)
                    } catch (e: Exception) {
                        MessageContentItem(type = contentType, payload = payload.toString())
                    }
                } else {
                    MessageContentItem(type = contentType, payload = payload.toString())
                }
            }
        }
    }
}
