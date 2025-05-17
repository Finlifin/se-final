package fin.phoenix.flix.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * 消息数据访问对象接口
 */
@Dao
interface MessageDao {

    /**
     * 获取指定会话的所有消息
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversationId(conversationId: String): LiveData<List<MessageEntity>>

    /**
     * 获取指定会话的所有消息，支持分页
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesByConversationIdPaged(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    /**
     * 获取指定ID的消息
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    /**
     * 获取指定客户端消息ID的消息
     */
    @Query("SELECT * FROM messages WHERE clientMessageId = :clientMessageId")
    suspend fun getMessageByClientId(clientMessageId: String): MessageEntity?
    
    /**
     * 获取指定会话的最后一条消息
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastMessageForConversation(conversationId: String): MessageEntity?
    
    /**
     * 获取所有处于发送中状态的消息
     */
    @Query("SELECT * FROM messages WHERE isSending = 1")
    suspend fun getPendingMessages(): List<MessageEntity>
    
    /**
     * 插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)
    
    /**
     * 批量插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)
    
    /**
     * 更新消息
     */
    @Update
    suspend fun update(message: MessageEntity)
    
    /**
     * 更新消息状态
     */
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    /**
     * 更新所有指定会话的未读消息为已读
     */
    @Query("UPDATE messages SET status = 'read' WHERE conversationId = :conversationId")
    suspend fun markConversationMessagesAsRead(conversationId: String)

    /**
     * 更新消息发送状态
     */
    @Query("UPDATE messages SET isSending = :isSending, status = :status, sendAttempts = :attempts, errorMessage = :errorMessage WHERE id = :messageId")
    suspend fun updateSendingStatus(messageId: String, isSending: Boolean, status: String, attempts: Int, errorMessage: String?)
    
    /**
     * 删除消息
     */
    @Delete
    suspend fun delete(message: MessageEntity)
    
    /**
     * 删除指定ID的消息
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    /**
     * 删除指定会话的所有消息
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)

    /**
     * 删除所有消息
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
    
    /**
     * 获取指定会话未读消息的数量
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND status = 'unread'")
    suspend fun getUnreadCount(conversationId: String): Int
    
    /**
     * 获取所有会话的总未读消息数
     */
    @Query("SELECT COUNT(*) FROM messages WHERE status = 'unread'")
    suspend fun getTotalUnreadCount(): Int
}