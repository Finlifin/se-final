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
            // 普通文本消息
            "text" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 系统通知
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

            // 图片消息
            "image" -> MessageContentItem(
                type = contentType,
                payload = payload.asString
            )

            // 商品信息
            "product" -> {
                val payloadObj = context.deserialize<ProductPayload>(
                    payload,
                    ProductPayload::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 订单信息
            "order" -> {
                val payloadObj = context.deserialize<OrderPayload>(
                    payload,
                    OrderPayload::class.java
                )
                MessageContentItem(type = contentType, payload = payloadObj)
            }

            // 未知类型,保持原始JSON
            else -> MessageContentItem(
                type = contentType,
                payload = payload.toString()
            )
        }
    }
}
