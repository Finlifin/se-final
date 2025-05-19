package fin.phoenix.flix.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import fin.phoenix.flix.data.UserAbstract

/**
 * UserAbstract实体类，用于Room数据库存储
 */
@Entity(tableName = "user_abstract")
data class UserAbstractEntity(
    @PrimaryKey
    val uid: String,
    val userName: String,
    val avatarUrl: String?,
    val schoolId: String?,
    val campusId: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 将实体转换为数据模型
     */
    fun toUserAbstract(): UserAbstract {
        return UserAbstract(
            uid = uid,
            userName = userName,
            avatarUrl = avatarUrl,
            schoolId = schoolId,
            campusId = campusId
        )
    }

    companion object {
        /**
         * 将数据模型转换为实体
         */
        fun fromUserAbstract(userAbstract: UserAbstract): UserAbstractEntity {
            return UserAbstractEntity(
                uid = userAbstract.uid,
                userName = userAbstract.userName,
                avatarUrl = userAbstract.avatarUrl,
                schoolId = userAbstract.schoolId,
                campusId = userAbstract.campusId
            )
        }
    }
}