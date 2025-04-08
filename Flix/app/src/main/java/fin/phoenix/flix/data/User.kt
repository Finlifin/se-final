package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName

/**
 * User data model class
 * Represents a user in the second-hand trading platform
 */
data class User(
    @SerializedName("uid") val uid: String, // User ID, unique identifier (primary key)
    @SerializedName("phone_number") val phoneNumber: String, // User's phone number
    @SerializedName("user_name") val userName: String, // Username
    @SerializedName("avatar_url") val avatarUrl: String? = null, // User avatar image URL
    @SerializedName("addresses") val addresses: List<String> = emptyList(), // List of user addresses
    @SerializedName("current_address") val currentAddress: String? = null, // Current selected address
    @SerializedName("balance") val balance: Int = 0, // User balance (virtual currency)
    @SerializedName("published_product_ids") val publishedProductIds: List<String> = emptyList(), // IDs of products published by user
    @SerializedName("sold_product_ids") val soldProductIds: List<String> = emptyList(), // IDs of products sold by user
    @SerializedName("purchased_product_ids") val purchasedProductIds: List<String> = emptyList(), // IDs of products purchased by user
    @SerializedName("favorited_product_ids") val favoritedProductIds: List<String> = emptyList(), // IDs of products favorited by user
    @SerializedName("school_id") val schoolId: String? = null, // User's school ID
    @SerializedName("campus_id") val campusId: String? = null  // User's campus ID
)

//seller = %{
//    uid: user.uid,
//    user_name: user.user_name,
//    avatar_url: user.avatar_url,
//    school_id: user.school_id,
//    campus_id: user.campus_id,
//    sold_count: length(user.sold_product_ids || [])
//}
data class Seller(
    @SerializedName("uid") val uid: String, // User ID
    @SerializedName("user_name") val userName: String, // Username
    @SerializedName("avatar_url") val avatarUrl: String? = null, // User avatar image URL
    @SerializedName("sold_product_ids") val soldProductIds: List<String> = emptyList(), // IDs of products sold by user
    @SerializedName("published_product_ids") val publishedProductIds: List<String> = emptyList(), // IDs of products published by user
    @SerializedName("current_address") val currentAddress: String? = null, // Current selected address
    @SerializedName("school_id") val schoolId: String? = null, // User's school ID
    @SerializedName("campus_id") val campusId: String? = null, // User's campus ID
    @SerializedName("sold_count") val soldCount: Int = 0 // Number of products sold by user
)