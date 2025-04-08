package fin.phoenix.flix.data

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom deserializer for MessageContent interface
 * This class handles proper deserialization of different message content types
 */
class MessageContentDeserializer : JsonDeserializer<MessageContent> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): MessageContent {
        val jsonObject = json.asJsonObject
        
        // First check if there's an explicit content_type field in the parent object
        // If not, determine the type based on available fields in the content
        return when {
            // Text message detection
            jsonObject.has("text") && (jsonObject.has("item_id") || jsonObject.has("image_urls")) -> 
                context.deserialize<TextMessageContent>(json, TextMessageContent::class.java)
            
            // System notification detection
            jsonObject.has("text") && jsonObject.has("title") && !jsonObject.has("item_id") && 
                    !jsonObject.has("interaction_type") -> 
                context.deserialize<SystemNotificationContent>(json, SystemNotificationContent::class.java)
            
            // System announcement - if needed to distinguish from notification based on some criteria
            // For now it will follow the same pattern as notification
            
            // Interaction message detection
            jsonObject.has("interaction_type") -> 
                context.deserialize<InteractionMessageContent>(json, InteractionMessageContent::class.java)
            
            // Default to text message if we can't determine the type
            else -> context.deserialize<TextMessageContent>(json, TextMessageContent::class.java)
        }
    }
}
