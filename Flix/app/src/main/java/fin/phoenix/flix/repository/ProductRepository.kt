package fin.phoenix.flix.repository

import android.content.Context
import android.net.Uri
import fin.phoenix.flix.api.CreateProductRequest
import fin.phoenix.flix.api.ProductListData
import fin.phoenix.flix.api.ProductService
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.api.UpdateProductRequest
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.ProductStatus
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 商品仓库类，处理与商品相关的业务逻辑
 */
class ProductRepository(context: Context) {

    private val productService = RetrofitClient.createService(ProductService::class.java, context)
    private val imageRepository = ImageRepository(context)

    /**
     * 获取商品列表
     */
    suspend fun getProducts(
        page: Int = 1,
        limit: Int = 10,
        category: String? = null,
        sellerId: String? = null,
        searchQuery: String? = null,
        priceRange: Pair<Double?, Double?>? = null,
        sortBy: String? = null,
        sortOrder: String? = null,
        availableStatuses: List<String> = listOf("available")
    ): Resource<ProductListData> = withContext(Dispatchers.IO) {
        try {
            val response = productService.getProducts(
                page = page,
                limit = limit,
                category = category,
                sellerId = sellerId,
                searchQuery = searchQuery,
                minPrice = priceRange?.first,
                maxPrice = priceRange?.second,
                sortBy = sortBy,
                sortOrder = sortOrder,
                availableStatuses = availableStatuses
            )
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    return@withContext Resource.Success(
                        ProductListData(
                            products = body.data,
                            totalCount = body.totalCount,
                            currentPage = body.currentPage,
                            totalPages = body.totalPages
                        )
                    )
                } else {
                    return@withContext Resource.Error(body.message)
                }
            } else {
                return@withContext Resource.Error("获取商品失败: ${response.message()}")
            }
        } catch (e: Exception) {
            return@withContext Resource.Error("获取商品失败: ${e.message}")
        }
    }

    /**
     * 获取单个商品详情
     */
    suspend fun getProductById(productId: String): Resource<Product> = withContext(Dispatchers.IO) {
        productService.getProductById(productId).toResource("获取商品详情失败")
    }

    /**
     * 发布商品（包括上传图片）
     */
    suspend fun publishProduct(
        sellerId: String,
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        tags: List<String>,
        availableDeliveryMethods: List<String>,
        location: String,
        imageUris: List<Uri>
    ): Resource<Product> = withContext(Dispatchers.IO) {
        try {
            // 先上传图片
            val imageResult = imageRepository.uploadImages(imageUris)
            
            if (imageResult.isFailure) {
                return@withContext Resource.Error(
                    imageResult.exceptionOrNull()?.message ?: "图片上传失败"
                )
            }
            
            val imageUrls = imageResult.getOrThrow()
            
            // 创建商品
            val request = CreateProductRequest(
                sellerId = sellerId,
                title = title,
                description = description,
                price = price,
                images = imageUrls,
                category = category,
                condition = condition,
                location = location,
                tags = tags,
                availableDeliveryMethods = availableDeliveryMethods
            )
            
            productService.createProduct(request).toResource("商品发布失败")
        } catch (e: Exception) {
            Resource.Error("商品发布失败: ${e.message}")
        }
    }

    /**
     * 更新商品信息
     */
    suspend fun updateProduct(
        productId: String,
        title: String? = null,
        description: String? = null,
        price: Double? = null,
        category: String? = null,
        condition: String? = null,
        location: String? = null,
        status: ProductStatus? = null,
        imageUris: List<Uri>? = null,
        tags: List<String>? = null,
        availableDeliveryMethods: List<String>? = null
    ): Resource<Product> = withContext(Dispatchers.IO) {
        try {
            // 如果有新图片，先上传
            val imageUrls = if (!imageUris.isNullOrEmpty()) {
                val imageResult = imageRepository.uploadImages(imageUris)
                if (imageResult.isFailure) {
                    return@withContext Resource.Error(
                        imageResult.exceptionOrNull()?.message ?: "图片上传失败"
                    )
                }
                imageResult.getOrThrow()
            } else null
            
            // 更新商品
            val request = UpdateProductRequest(
                title = title,
                description = description,
                price = price,
                category = category,
                condition = condition,
                location = location,
                status = status?.name,
                images = imageUrls,
                tags = tags,
                availableDeliveryMethods = availableDeliveryMethods
            )
            
            productService.updateProduct(productId, request).toResource("商品更新失败")
        } catch (e: Exception) {
            Resource.Error("商品更新失败: ${e.message}")
        }
    }

    /**
     * 删除商品
     */
    suspend fun deleteProduct(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        productService.deleteProduct(productId).toResource("商品删除失败")
    }


    suspend fun isProductFavorite(productId: String): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            val response = productService.isProductFavorite(productId).toResource("获取收藏状态失败")
            if (response is Resource.Success) {
                Resource.Success(response.data)
            } else if (response is Resource.Error) {
                response
            } else {
                Resource.Error("获取收藏状态失败")
            }
        }

    /**
     * 收藏商品
     */
    suspend fun favoriteProduct(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        productService.favoriteProduct(productId).toResource("收藏商品失败")
    }

    /**
     * 取消收藏商品
     */
    suspend fun unfavoriteProduct(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        productService.unfavoriteProduct(productId).toResource("取消收藏失败")
    }

    /**
     * 获取收藏的商品列表
     */
    suspend fun getFavoriteProducts(
        userId: String,
        page: Int = 1,
        limit: Int = 10
    ): Resource<ProductListData> = withContext(Dispatchers.IO) {
        try {
            val response = productService.getFavoriteProducts(userId, page, limit)
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    return@withContext Resource.Success(
                        ProductListData(
                            products = body.data,
                            totalCount = body.totalCount,
                            currentPage = body.currentPage,
                            totalPages = body.totalPages
                        )
                    )
                } else {
                    return@withContext Resource.Error(body.message)
                }
            } else {
                return@withContext Resource.Error("获取收藏商品失败: ${response.message()}")
            }
        } catch (e: Exception) {
            return@withContext Resource.Error("获取收藏商品失败: ${e.message}")
        }
    }
}

