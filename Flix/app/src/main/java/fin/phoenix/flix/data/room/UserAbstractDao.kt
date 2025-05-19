package fin.phoenix.flix.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * UserAbstract数据访问对象
 */
@Dao
interface UserAbstractDao {
    
    /**
     * 插入或更新用户简要信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAbstract(userAbstract: UserAbstractEntity)
    
    /**
     * 根据用户ID获取用户简要信息
     */
    @Query("SELECT * FROM user_abstract WHERE uid = :userId")
    suspend fun getUserAbstract(userId: String): UserAbstractEntity?
    
    /**
     * 删除超过指定时间的缓存数据
     */
    @Query("DELETE FROM user_abstract WHERE timestamp < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
}