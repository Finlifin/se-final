package fin.phoenix.flix.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * 会话数据访问对象接口
 */
@Dao
interface ConversationDao {
    
    /**
     * 获取所有会话
     */
    @Query("SELECT * FROM conversations ORDER BY (SELECT createdAt FROM messages WHERE messages.id = conversations.lastMessageId) DESC")
    fun getAllConversations(): LiveData<List<ConversationEntity>>
    
    /**
     * 获取指定ID的会话
     */
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?
    
    /**
     * 获取与指定用户的会话
     */
    @Query("SELECT * FROM conversations WHERE participantId = :participantId")
    suspend fun getConversationByParticipantId(participantId: String): ConversationEntity?

    /**
     * 插入会话
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)
    
    /**
     * 批量插入会话
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)
    
    /**
     * 更新会话
     */
    @Update
    suspend fun update(conversation: ConversationEntity)
    
    /**
     * 更新会话的最后一条消息ID
     */
    @Query("UPDATE conversations SET lastMessageId = :messageId WHERE id = :conversationId")
    suspend fun updateLastMessageId(conversationId: String, messageId: String)
    
    /**
     * 更新会话未读消息数
     */
    @Query("UPDATE conversations SET unreadCounts = :unreadCounts WHERE id = :conversationId")
    suspend fun updateUnreadCounts(conversationId: String, unreadCounts: Int)
    
    /**
     * 增加会话未读消息计数
     */
    @Query("UPDATE conversations SET unreadCounts = unreadCounts + 1 WHERE id = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String)
    
    /**
     * 清空会话未读消息计数
     */
    @Query("UPDATE conversations SET unreadCounts = 0 WHERE id = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)
    
    /**
     * 删除会话
     */
    @Delete
    suspend fun delete(conversation: ConversationEntity)
    
    /**
     * 删除指定ID的会话
     */
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteById(conversationId: String)
    
    /**
     * 删除所有会话
     */
    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
    
    /**
     * 事务：插入会话及其关联的最后一条消息
     */
    @Transaction
    suspend fun insertWithLastMessage(conversation: ConversationEntity, lastMessageEntity: MessageEntity) {
        insert(conversation)
        // 最后一条消息通过MessageDao处理
    }
}