package fin.phoenix.flix.api

import fin.phoenix.flix.data.Seller
import fin.phoenix.flix.data.User
import fin.phoenix.flix.data.UserAbstract
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
    suspend fun updateUserProfile(@Body user: User): Response<GenericApiResponse<User>>
    
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
        @Body avatarUrl: String
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
