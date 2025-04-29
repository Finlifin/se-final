package fin.phoenix.flix.api

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
    @GET("products")
    suspend fun getProducts(
        @Query("offset") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("category") category: String? = null,
        @Query("seller_id") sellerId: String? = null,
        @Query("search") searchQuery: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null
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

/**
 * 创建商品请求
 */
data class CreateProductRequest(
    val sellerId: String,
    val title: String,
    val description: String,
    val price: Double,
    val images: List<String>,
    val category: String,
    val condition: String,
    val location: String,
    val tags: List<String>,
    val availableDeliveryMethods: List<String>,
)

/**
 * 更新商品请求
 */
data class UpdateProductRequest(
    val title: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val images: List<String>? = null,
    val category: String? = null,
    val condition: String? = null,
    val location: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val availableDeliveryMethods: List<String>? = null,
)

/**
 * 基础响应结构
 */
data class BaseResponse(
    override val success: Boolean, override val message: String? = null
) : ApiResponse
