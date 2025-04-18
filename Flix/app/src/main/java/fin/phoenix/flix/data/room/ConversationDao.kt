package fin.phoenix.flix.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY CASE WHEN isPinned = 1 THEN 0 ELSE 1 END, updatedAt DESC LIMIT :limit")
    fun getAllConversations(limit: Int = 20): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId")
    suspend fun getConversationByConversationId(conversationId: String): ConversationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Query("UPDATE conversations SET unreadCount = :count WHERE id = :id")
    suspend fun updateUnreadCount(id: String, count: Int)
    
    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean)
    
    @Query("UPDATE conversations SET isMuted = :isMuted WHERE id = :id")
    suspend fun updateMuted(id: String, isMuted: Boolean)
    
    @Query("UPDATE conversations SET lastReadMessageId = :messageId WHERE id = :id")
    suspend fun updateLastReadMessage(id: String, messageId: String)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
    
    @Query("SELECT COUNT(*) FROM conversations WHERE unreadCount > 0")
    fun getTotalUnreadCount(): Flow<Int>
}