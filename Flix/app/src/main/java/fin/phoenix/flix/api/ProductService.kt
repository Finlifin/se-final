package fin.phoenix.flix.api

import com.google.gson.annotations.SerializedName
import fin.phoenix.flix.data.Product
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query


/**
 * 商品API服务接口
 */
interface ProductService {
    @POST("products")
    suspend fun getProducts(
        @Query("offset") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("category") category: String? = null,
        @Query("seller_id") sellerId: String? = null,
        @Query("search") searchQuery: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null,
        @Query("school_id") schoolId: String? = null,
        @Query("campus_id") campusId: String? = null,
        @Body availableStatuses: List<String>? = null,
    ): Response<ProductListResponse>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") productId: String): Response<GenericApiResponse<Product>>

    @POST("products/publish")
    suspend fun createProduct(@Body request: CreateProductRequest): Response<GenericApiResponse<Product>>

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") productId: String, @Body request: UpdateProductRequest
    ): Response<GenericApiResponse<Product>>

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") productId: String): Response<GenericApiResponse<Unit>>

    @GET("products/{id}/is_favorite")
    suspend fun isProductFavorite(@Path("id") productId: String): Response<GenericApiResponse<Boolean>>

    @POST("products/{id}/favorite")
    suspend fun favoriteProduct(@Path("id") productId: String): Response<GenericApiResponse<Unit>>

    @DELETE("products/{id}/favorite")
    suspend fun unfavoriteProduct(@Path("id") productId: String): Response<GenericApiResponse<Unit>>

    @GET("profile/favorites")
    suspend fun getFavoriteProducts(
        @Query("user_id") userId: String,
        @Query("offset") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<ProductListResponse>
}

/**
 * 商品列表响应
 */
data class ProductListResponse(
    val data: List<Product>,
    val message: String,
    val success: Boolean,
    val currentPage: Int,
    val totalPages: Int,
    val totalCount: Int
)

/**
 * 商品列表数据
 */
data class ProductListData(
    val products: List<Product> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

data class CreateProductRequest(
    @SerializedName("seller_id") val sellerId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: Double,
    @SerializedName("images") val images: List<String>,
    @SerializedName("category") val category: String,
    @SerializedName("condition") val condition: String,
    @SerializedName("location") val location: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("available_delivery_methods") val availableDeliveryMethods: List<String>,
    @SerializedName("campus_id") val campusId: String?
)

data class UpdateProductRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("images") val images: List<String>? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("condition") val condition: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("available_delivery_methods") val availableDeliveryMethods: List<String>? = null,
    @SerializedName("campus_id") val campusId: String? = null
)
