package fin.phoenix.flix.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import fin.phoenix.flix.util.imageUrl
import kotlinx.parcelize.Parcelize

/**
 * UserAbstract data model class
 * Simplified user information for list displays, reducing data transfer
 */
@Parcelize
data class UserAbstract(
    @SerializedName("uid") val uid: String, // User ID
    @SerializedName("user_name") val userName: String, // Username
    @SerializedName("avatar_url") val avatarUrl: String?, // User avatar image URL
    @SerializedName("school_id") val schoolId: String? = null, // User's school ID
    @SerializedName("campus_id") val campusId: String? = null  // User's campus ID
) : Parcelable

val loadingUserAbstract = UserAbstract(
    uid = "loading", userName = "loading", avatarUrl = imageUrl("loading_avatar.png")
)
