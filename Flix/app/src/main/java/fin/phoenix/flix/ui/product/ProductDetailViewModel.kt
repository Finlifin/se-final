package fin.phoenix.flix.ui.product

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
                        (productResponse as Resource.Error).message ?: "Unknown error"
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
//            val isFavorite = userRepository.isProductFavorite(productId)
            _isProductFavorite.value = false
        }
    }

    fun toggleFavorite() {
        val productId = (_productState.value as? Resource.Success)?.data?.id ?: return
        val currentFavoriteStatus = _isProductFavorite.value ?: false

        viewModelScope.launch {
            try {
//                if (currentFavoriteStatus) {
//                    userRepository.unfavoriteProduct(productId)
//                } else {
//                    userRepository.favoriteProduct(productId)
//                }
//                // 更新收藏状态
//                _isProductFavorite.value = !currentFavoriteStatus
            } catch (e: Exception) {
                // 处理错误，可能显示一个通知给用户
                Log.e("ProductDetailViewModel", "Error toggling favorite: ${e.message}")
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
                        onError((paymentResponse as Resource.Error).message ?: "设置支付信息失败")
                    }
                } else {
                    // 创建订单失败
                    onError((createOrderResponse as Resource.Error).message ?: "创建订单失败")
                }
            } catch (e: Exception) {
                onError("创建订单失败: ${e.message}")
            }
        }
    }
}
