package fin.phoenix.flix.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY serverTimestamp DESC LIMIT :limit")
    fun getMessagesForConversation(conversationId: String, limit: Int = 20): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND insertedAt < :before ORDER BY serverTimestamp DESC LIMIT :limit")
    fun getMessagesForConversationBefore(conversationId: String, before: Long, limit: Int = 20): Flow<List<MessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)

    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Query("UPDATE messages SET status = :newStatus WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: String)
    
    @Query("UPDATE messages SET status = :newStatus WHERE conversationId = :conversationId AND status = 'unread' AND senderId != :userId")
    suspend fun updateAllUnreadMessagesInConversation(conversationId: String, newStatus: String, userId: String? = null)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    @Query("SELECT * FROM messages WHERE clientMessageId = :clientMessageId")
    suspend fun getMessageByClientId(clientMessageId: String): MessageEntity?
    
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND status = 'unread' AND receiverId = :userId")
    fun getUnreadCountForConversation(conversationId: String, userId: String): Flow<Int>
}