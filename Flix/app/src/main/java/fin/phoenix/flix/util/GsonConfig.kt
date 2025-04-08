package fin.phoenix.flix.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import fin.phoenix.flix.data.MessageContent
import fin.phoenix.flix.data.MessageContentDeserializer
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
            .registerTypeAdapter(MessageContent::class.java, MessageContentDeserializer())
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .create()
    }
    
    /**
     * Creates a Gson instance with pretty printing for debugging purposes
     */
    fun createPrettyGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(MessageContent::class.java, MessageContentDeserializer())
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .setPrettyPrinting()
            .create()
    }
    
    /**
     * Simple Date adapter for proper date serialization/deserialization
     * You may need to adjust this based on your date format requirements
     */
    class DateTypeAdapter : com.google.gson.JsonSerializer<Date>, com.google.gson.JsonDeserializer<Date> {
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        override fun serialize(src: Date, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
            return com.google.gson.JsonPrimitive(src.time)
        }

        override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): Date {
            return try {
                // Try parsing as a long timestamp first
                Date(json.asJsonPrimitive.asLong)
            } catch (e: NumberFormatException) {
                try {
                    // If that fails, try parsing as an ISO string
                    val dateStr = json.asJsonPrimitive.asString
                    isoDateFormat.parse(dateStr) ?: Date()
                } catch (e: Exception) {
                    // If all parsing fails, return current date
                    Date()
                }
            }
        }
    }
}
