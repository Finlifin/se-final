package fin.phoenix.flix.repository

import android.content.Context
import android.util.Log
import fin.phoenix.flix.api.ProfileService
import fin.phoenix.flix.api.ProfileUpdateRequest
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.Seller
import fin.phoenix.flix.data.User
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.data.room.AppDatabase
import fin.phoenix.flix.data.room.UserAbstractDao
import fin.phoenix.flix.data.room.UserAbstractEntity
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户仓库类，处理与用户相关的业务逻辑
 */
class ProfileRepository(private val context: Context) {
    private val userService = RetrofitClient.createService(ProfileService::class.java, context)
    private val productRepository = ProductRepository(context)
    private val userManager = UserManager.getInstance(context)
    private val currentUserId = userManager.currentUserId.value

    // 缓存有效期 (毫秒)
    private val CACHE_VALID_TIME = 1000 * 60 * 10 // 10分钟

    // 获取用户数据库实例和DAO
    private fun getUserAbstractDao(): UserAbstractDao? {
        return try {
            val userId = currentUserId ?: return null
            val db = AppDatabase.getInstance(context, userId)
            db.userAbstractDao()
        } catch (e: Exception) {
            Log.e("ProfileRepository", "获取UserAbstractDao失败", e)
            null
        }
    }

    /**
     * 获取热门卖家
     */
    suspend fun getPopularSellers(limit: Int = 8): Resource<List<UserAbstract>> =
        withContext(Dispatchers.IO) {
            userService.getPopularSellers(limit).toResource("获取热门卖家失败")
        }

    /**
     * 根据用户ID获取用户简要信息
     * 首先尝试从本地缓存获取，如果缓存不存在或已过期，则从网络获取并更新缓存
     */
    suspend fun getUserAbstract(userId: String): Resource<UserAbstract> =
        withContext(Dispatchers.IO) {
            try {
                // 1. 尝试从缓存获取
                val dao = getUserAbstractDao()
                if (dao != null) {
                    val cachedEntity = dao.getUserAbstract(userId)

                    // 检查缓存是否有效
                    val now = System.currentTimeMillis()
                    if (cachedEntity != null && (now - cachedEntity.timestamp < CACHE_VALID_TIME)) {
                        Log.d("ProfileRepository", "从缓存获取用户信息成功: $userId")
                        return@withContext Resource.Success(cachedEntity.toUserAbstract())
                    }
                }

                // 2. 从网络获取
                val response = userService.getUserAbstract(userId).toResource("获取用户信息失败")

                // 3. 如果网络请求成功，更新缓存
                if (response is Resource.Success) {
                    dao?.insertUserAbstract(UserAbstractEntity.fromUserAbstract(response.data))

                    // 4. 定期清理过期缓存
                    val cleanupTime = System.currentTimeMillis() - CACHE_VALID_TIME * 2
                    dao?.deleteOldCache(cleanupTime)
                }

                response
            } catch (e: Exception) {
                Log.e("ProfileRepository", "获取用户信息异常", e)
                Resource.Error("获取用户信息失败: ${e.message}")
            }
        }

    /**
     * 获取用户完整资料
     */
    suspend fun getUserProfile(userId: String): Resource<User> = withContext(Dispatchers.IO) {
        userService.getUserProfile(userId).toResource("获取用户资料失败")
    }

    /**
     * 获取商家信息
     */
    suspend fun getSellerProfile(userId: String): Resource<Seller> = withContext(Dispatchers.IO) {
        userService.getSellerProfile(userId).toResource("获取商家信息失败")
    }

    /**
     * 更新用户信息
     */
    suspend fun updateUserProfile(user: ProfileUpdateRequest): Resource<User> =
        withContext(Dispatchers.IO) {
            userService.updateUserProfile(user).toResource("更新用户信息失败")
        }

    /**
     * 充值余额
     */
    suspend fun rechargeBalance(userId: String, amount: Int): Resource<User> =
        withContext(Dispatchers.IO) {
            userService.rechargeBalance(userId, amount).toResource("充值失败")
        }

    /**
     * 更新用户头像
     */
    suspend fun updateAvatar(userId: String, avatarUrl: String): Resource<User> =
        withContext(Dispatchers.IO) {
            userService.updateAvatar(userId, avatarUrl).toResource("更新头像失败")
        }

    // 照理说，这些接口都应该只返回AbstractProduct，而不是Product
    // 历史缘故（我懒），这里就先返回Product了
    /**
     * 获取用户发布的商品列表
     */
    suspend fun getUserProducts(userId: String): Resource<List<Product>> =
        withContext(Dispatchers.IO) {
            val response = userService.getUserProducts(userId).toResource("获取用户商品失败")
            if (response is Resource.Success) {
                val products = mutableListOf<Product>()
                for (productId in response.data) {
                    val product = productRepository.getProductById(productId)
                    if (product is Resource.Success) {
                        products.add(product.data)
                    }
                }
                Resource.Success(products)
            } else if (response is Resource.Error) {
                response
            } else {
                Resource.Error("获取用户商品失败")
            }
        }

    /**
     * 获取用户已售商品列表
     */
    suspend fun getUserSoldProducts(userId: String): Resource<List<Product>> =
        withContext(Dispatchers.IO) {
            val response =
                userService.getUserSoldProducts(userId).toResource("获取用户已售商品失败")
            if (response is Resource.Success) {
                val products = mutableListOf<Product>()
                for (productId in response.data) {
                    val product = productRepository.getProductById(productId)
                    if (product is Resource.Success) {
                        products.add(product.data)
                    }
                }
                Resource.Success(products)
            } else if (response is Resource.Error) {
                response
            } else {
                Resource.Error("获取用户已售商品失败")
            }
        }

    /**
     * 获取用户购买的商品列表
     */
    suspend fun getUserPurchasedProducts(userId: String): Resource<List<Product>> =
        withContext(Dispatchers.IO) {
            val response =
                userService.getUserPurchasedProducts(userId).toResource("获取用户购买商品失败")
            if (response is Resource.Success) {
                val products = mutableListOf<Product>()
                for (productId in response.data) {
                    val product = productRepository.getProductById(productId)
                    if (product is Resource.Success) {
                        products.add(product.data)
                    }
                }
                Resource.Success(products)
            } else if (response is Resource.Error) {
                response
            } else {
                Resource.Error("获取用户购买商品失败")
            }
        }

    /**
     * 获取用户收藏的商品列表
     */
    suspend fun getUserFavorites(userId: String): Resource<List<Product>> =
        withContext(Dispatchers.IO) {
            val response = userService.getUserFavorites(userId).toResource("获取用户收藏失败")
            if (response is Resource.Success) {
                val products = mutableListOf<Product>()
                for (productId in response.data) {
                    val product = productRepository.getProductById(productId)
                    if (product is Resource.Success) {
                        products.add(product.data)
                    }
                }
                Resource.Success(products)
            } else if (response is Resource.Error) {
                response
            } else {
                Resource.Error("获取用户收藏失败")
            }
        }
}
