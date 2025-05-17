package fin.phoenix.flix.api

import com.google.gson.annotations.SerializedName
import fin.phoenix.flix.data.Seller
import fin.phoenix.flix.data.User
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.util.Resource
import retrofit2.Response
import retrofit2.http.*

/**
 * 用户相关API服务接口
 */
interface ProfileService {
    /**
     * 获取用户完整资料
     */
    @GET("profile/user/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<GenericApiResponse<User>>

    /**
     * 获取商家信息
     */
    @GET("profile/seller/{userId}")
    suspend fun getSellerProfile(@Path("userId") userId: String): Response<GenericApiResponse<Seller>>

    /**
     * 获取用户简要信息
     */
    @GET("profile/abstract/{userId}")
    suspend fun getUserAbstract(@Path("userId") userId: String): Response<GenericApiResponse<UserAbstract>>
    
    /**
     * 获取热门卖家列表
     */
    @GET("profile/popular")
    suspend fun getPopularSellers(@Query("limit") limit: Int): Response<GenericApiResponse<List<UserAbstract>>>
    
    /**
     * 更新用户资料
     */
    @PUT("profile/update")
    suspend fun updateUserProfile(@Body user: ProfileUpdateRequest): Response<GenericApiResponse<User>>
    
    /**
     * 充值余额
     */
    @POST("profile/recharge")
    suspend fun rechargeBalance(
        @Query("userId") userId: String,
        @Query("amount") amount: Int
    ): Response<GenericApiResponse<User>>
    
    /**
     * 更新用户头像
     */
    @POST("profile/avatar")
    suspend fun updateAvatar(
        @Query("userId") userId: String,
        @Query("avatarUrl") avatarUrl: String
    ): Response<GenericApiResponse<User>>
    
    /**
     * 获取用户发布的商品
     */
    @GET("profile/{userId}/products")
    suspend fun getUserProducts(@Path("userId") userId: String): Response<GenericApiResponse<List<String>>>
    
    /**
     * 获取用户已售商品
     */
    @GET("profile/{userId}/sold")
    suspend fun getUserSoldProducts(@Path("userId") userId: String): Response<GenericApiResponse<List<String>>>
    
    /**
     * 获取用户购买的商品
     */
    @GET("profile/{userId}/purchased")
    suspend fun getUserPurchasedProducts(@Path("userId") userId: String): Response<GenericApiResponse<List<String>>>
    
    /**
     * 获取用户收藏的商品
     */
    @GET("profile/{userId}/favorites")
    suspend fun getUserFavorites(@Path("userId") userId: String): Response<GenericApiResponse<List<String>>>
}


data class ProfileUpdateRequest(
    @SerializedName("uid") val uid: String, // User ID, unique identifier (primary key)
    @SerializedName("phone_number") val phoneNumber: String, // User's phone number
    @SerializedName("user_name") val userName: String, // Username
    @SerializedName("avatar_url") val avatarUrl: String? = null, // User avatar image URL
    @SerializedName("addresses") val addresses: List<String> = emptyList(), // List of user addresses
    @SerializedName("current_address") val currentAddress: String? = null, // Current selected address
    @SerializedName("school_id") val schoolId: String? = null, // User's school ID
    @SerializedName("campus_id") val campusId: String? = null  // User's campus ID
)

fun User.toUpdateRequest(): ProfileUpdateRequest {
    return ProfileUpdateRequest(
        uid = uid,
        phoneNumber = phoneNumber,
        userName = userName,
        avatarUrl = avatarUrl,
        addresses = addresses,
        currentAddress = currentAddress,
        schoolId = schoolId,
        campusId = campusId
    )
}

fun Resource<User>.toProfileUpdateRequestResource(): Resource<ProfileUpdateRequest> {
    return when (this) {
        is Resource.Success -> Resource.Success(data.toUpdateRequest())
        is Resource.Error -> Resource.Error(message)
        is Resource.Loading -> Resource.Loading
    }
}