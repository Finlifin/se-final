package fin.phoenix.flix.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 应用数据库类
 * 该类管理为不同用户维护的隔离数据库
 */
@Database(
    entities = [MessageEntity::class, ConversationEntity::class, UserAbstractEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun userAbstractDao(): UserAbstractDao

    companion object {
        private const val DB_NAME_PREFIX = "flix_db_"

        @Volatile
        private var INSTANCES = mutableMapOf<String, AppDatabase>()

        /**
         * 获取特定用户的数据库实例
         * @param context 上下文
         * @param userId 用户ID
         * @return 数据库实例
         */
        fun getInstance(context: Context, userId: String): AppDatabase {
            val dbName = "${DB_NAME_PREFIX}${userId}"
            return INSTANCES.getOrPut(userId) {
                synchronized(this) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        dbName
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                }
            }
        }

        /**
         * 清理特定用户的数据库实例
         * @param userId 用户ID
         */
        fun clearUserDatabase(userId: String) {
            INSTANCES.remove(userId)
        }
    }
}