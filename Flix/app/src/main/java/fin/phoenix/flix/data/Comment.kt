package fin.phoenix.flix.data

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * 评论数据类
 */
@SuppressLint("ParcelCreator")
@Parcelize
data class Comment(
    val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("parent_id") val parentId: String? = null,
    @SerializedName("root_id") val rootId: String? = null,
    val content: String,
    @SerializedName("likes_count") var likesCount: Int = 0,
    @SerializedName("replies_count") val repliesCount: Int = 0,
    val status: CommentStatus = CommentStatus.ACTIVE,
    @SerializedName("inserted_at") val createdAt: Date,
    @SerializedName("updated_at") val updatedAt: Date,
    val user: UserAbstract? = null,
    // 客户端状态，不从服务端获取
    @SerializedName("is_liked") var isLiked: Boolean = false,
    var isHighlighted: Boolean = false
) : Parcelable

/**
 * 评论状态枚举
 */
enum class CommentStatus {
    @SerializedName("active") ACTIVE,
    @SerializedName("deleted") DELETED,
    @SerializedName("hidden") HIDDEN
}

/**
 * 评论上下文数据类，用于显示一条评论及其所在的评论树
 */
@Parcelize
data class CommentContext(
    @SerializedName("root_comment") val rootComment: Comment,
    val replies: List<Comment>,
    @SerializedName("target_comment_id") val targetCommentId: String
) : Parcelable