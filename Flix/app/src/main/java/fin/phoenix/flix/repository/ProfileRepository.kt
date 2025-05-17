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

    /**
     * 获取热门卖家
     */
    suspend fun getPopularSellers(limit: Int = 8): Resource<List<UserAbstract>> =
        withContext(Dispatchers.IO) {
            userService.getPopularSellers(limit).toResource("获取热门卖家失败")
        }

    /**
     * 根据用户ID获取用户简要信息
     */
    suspend fun getUserAbstract(userId: String): Resource<UserAbstract> =
        withContext(Dispatchers.IO) {
            userService.getUserAbstract(userId).toResource("获取用户信息失败")
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
