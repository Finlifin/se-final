package fin.phoenix.flix.ui.product

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.repository.OrderRepository
import fin.phoenix.flix.repository.PaymentRepository
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch
import java.io.IOException

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ProductDetailViewModel"
    private val productRepository = ProductRepository(application.applicationContext)
    private val userRepository = ProfileRepository(application.applicationContext)
    private val orderRepository = OrderRepository(application.applicationContext)
    private val paymentRepository = PaymentRepository(application.applicationContext)

    val currentUserId = application.applicationContext.getSharedPreferences("flix_prefs", 0)
        .getString("user_id", null) ?: ""

    private val _productState = MutableLiveData<Resource<Product>>()
    val productState: LiveData<Resource<Product>> = _productState

    private val _sellerState = MutableLiveData<Resource<UserAbstract>>()
    val sellerState: LiveData<Resource<UserAbstract>> = _sellerState

    private val _isProductFavorite = MutableLiveData(false)
    val isProductFavorite: LiveData<Boolean> = _isProductFavorite

    fun loadProductDetails(productId: String) {
        _productState.value = Resource.Loading
        viewModelScope.launch {
            try {
                // 获取商品详情
                val productResponse = productRepository.getProductById(productId)
                if (productResponse is Resource.Success) {
                    _productState.value = productResponse
                    // 使用商品的卖家ID加载卖家信息
                    val product = productResponse.data
                    loadSellerDetails(product.sellerId)
                    // 检查商品是否被收藏
                    checkIfProductIsFavorite(productId)
                } else {
                    _productState.value = Resource.Error(
                        (productResponse as Resource.Error).message
                    )
                }
            } catch (e: IOException) {
                _productState.value = Resource.Error("网络连接失败: ${e.message}")
            } catch (e: Exception) {
                _productState.value = Resource.Error("获取商品信息失败: ${e.message}")
            }
        }
    }

    private fun loadSellerDetails(sellerId: String) {
        _sellerState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val sellerResponse = userRepository.getUserById(sellerId)
                Log.d(TAG, sellerResponse.toString())
                _sellerState.value = sellerResponse
            } catch (e: Exception) {
                _sellerState.value = Resource.Error("获取卖家信息失败: ${e.message}")
            }
        }
    }

    private fun checkIfProductIsFavorite(productId: String) {
        viewModelScope.launch {
            try {
                val response = productRepository.isProductFavorite(productId)
                if (response is Resource.Success) {
                    Log.d(TAG, "Product favorite status: ${response.data}")
                    _isProductFavorite.value = response.data
                } else {
                    _isProductFavorite.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking favorite status: ${e.message}")
                _isProductFavorite.value = false
            }
        }
    }

    // BUG: 这里的收藏状态没有更新
    fun toggleFavorite() {
        val productId = (_productState.value as? Resource.Success)?.data?.id ?: return
        val currentFavoriteStatus = _isProductFavorite.value ?: false

        viewModelScope.launch {
            _isProductFavorite.value = !currentFavoriteStatus  // Optimistic update
            try {
                val result = if (currentFavoriteStatus) {
                    productRepository.unfavoriteProduct(productId)
                } else {
                    productRepository.favoriteProduct(productId)
                }

                when (result) {
                    is Resource.Error -> {
                        _isProductFavorite.value = currentFavoriteStatus  // Revert on error
                        Log.e(TAG, "Error toggling favorite: ${result.message}")
                    }

                    is Resource.Success -> {
                        // State is already updated optimistically
                    }

                    else -> _isProductFavorite.value =
                        currentFavoriteStatus  // Revert on unknown state
                }
            } catch (e: Exception) {
                _isProductFavorite.value = currentFavoriteStatus  // Revert on exception
                Log.e(TAG, "Error toggling favorite: ${e.message}")
            }
        }
    }


    // 创建订单
    fun createOrder(
        productId: String,
        paymentMethod: String,
        deliveryMethod: String,
        address: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 第一步：创建订单
                val createOrderResponse = orderRepository.createOrder(productId)
                if (createOrderResponse is Resource.Success) {
                    val orderId = createOrderResponse.data.orderId

                    // 第二步：设置支付和配送信息
                    val paymentResponse = paymentRepository.setupPayment(
                        orderId = orderId,
                        paymentMethod = paymentMethod,
                        deliveryMethod = deliveryMethod,
                        deliveryAddress = address
                    )

                    if (paymentResponse is Resource.Success) {
                        // 支付设置成功，返回订单ID用于跳转到支付确认页面
                        onSuccess(orderId)
                    } else {
                        // 支付设置失败
                        onError((paymentResponse as Resource.Error).message)
                    }
                } else {
                    // 创建订单失败
                    onError((createOrderResponse as Resource.Error).message)
                }
            } catch (e: Exception) {
                onError("创建订单失败: ${e.message}")
            }
        }
    }
}
