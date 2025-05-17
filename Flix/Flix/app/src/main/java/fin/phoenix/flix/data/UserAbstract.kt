package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName
import fin.phoenix.flix.util.imageUrl

/**
 * UserAbstract data model class
 * Simplified user information for list displays, reducing data transfer
 */
data class UserAbstract(
    @SerializedName("uid") val uid: String, // User ID
    @SerializedName("user_name") val userName: String, // Username
    @SerializedName("avatar_url") val avatarUrl: String? // User avatar image URL
)

val loadingUserAbstract = UserAbstract(
    uid = "loading", userName = "loading", avatarUrl = imageUrl("loading_avatar.png")
)
