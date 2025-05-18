package fin.phoenix.flix.ui.product

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Campus
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.repository.OrderRepository
import fin.phoenix.flix.repository.PaymentRepository
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.repository.SchoolRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch
import java.io.IOException

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ProductDetailViewModel"
    private val productRepository = ProductRepository(application.applicationContext)
    private val userRepository = ProfileRepository(application.applicationContext)
    private val orderRepository = OrderRepository(application.applicationContext)
    private val paymentRepository = PaymentRepository(application.applicationContext)
    private val schoolRepository = SchoolRepository(application.applicationContext)

    val currentUserId = application.applicationContext.getSharedPreferences("flix_prefs", 0)
        .getString("user_id", null) ?: ""

    private val _productState = MutableLiveData<Resource<Product>>()
    val productState: LiveData<Resource<Product>> = _productState

    private val _sellerState = MutableLiveData<Resource<UserAbstract>>()
    val sellerState: LiveData<Resource<UserAbstract>> = _sellerState

    private val _isProductFavorite = MutableLiveData(false)
    val isProductFavorite: LiveData<Boolean> = _isProductFavorite

    private val _campusState = MutableLiveData<Resource<Campus>>()
    val campusState: LiveData<Resource<Campus>> = _campusState

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
                    // 如果商品有校区ID，加载校区信息
                    if (product.campusId != null) {
                        loadCampusDetails(product.campusId)
                    } else {
                        _campusState.value = Resource.Loading
                    }
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
                val sellerResponse = userRepository.getUserAbstract(sellerId)
                Log.d(TAG, sellerResponse.toString())
                _sellerState.value = sellerResponse
            } catch (e: Exception) {
                _sellerState.value = Resource.Error("获取卖家信息失败: ${e.message}")
            }
        }
    }

    private fun loadCampusDetails(campusId: String) {
        _campusState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val campusResponse = schoolRepository.getCampus(campusId)
                _campusState.value = campusResponse
            } catch (e: Exception) {
                _campusState.value = Resource.Error("获取校区信息失败: ${e.message}")
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

                // 收藏操作即使返回Resource.Error，只要message中包含"成功"字样，我们也认为是成功的
                when (result) {
                    is Resource.Error -> {
                        // 检查错误消息是否包含成功字样
                        if (result.message.contains("成功")) {
                            // 这是一个误报的错误，实际操作成功了
                            Log.d(TAG, "收藏操作成功: ${result.message}")
                            // 保持乐观更新的状态
                        } else {
                            // 真的出错了，回退状态
                            _isProductFavorite.value = currentFavoriteStatus  // Revert on error
                            Log.e(TAG, "Error toggling favorite: ${result.message}")
                        }
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

    /**
     * 清理所有资源和状态，避免内存泄漏和UI残留
     * 在用户离开ProductDetailScreen时调用
     */
    fun clearResources() {
        // 重置所有状态为初始值
        _productState.value = Resource.Loading
        _sellerState.value = Resource.Loading
        _isProductFavorite.value = false
        _campusState.value = Resource.Loading
        // 取消所有可能的网络请求
        viewModelScope.launch {
            // 这里可以添加取消任何正在进行的网络请求
            // 如果有使用协程Job，可以在这里取消
        }
    }
}
