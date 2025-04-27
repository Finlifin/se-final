package fin.phoenix.flix.ui.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.OrderDetails
import fin.phoenix.flix.repository.OrderRepository
import fin.phoenix.flix.repository.PaymentRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val paymentRepository = PaymentRepository(application.applicationContext)
    private val orderRepository = OrderRepository(application.applicationContext)

    // 支付状态
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Loading)
    val paymentState: StateFlow<PaymentState> = _paymentState

    // 订单状态
    private val _orderState = MutableStateFlow<OrderState>(OrderState.Loading)
    val orderState: StateFlow<OrderState> = _orderState

    // 获取支付状态
    fun getPaymentStatus(orderId: String) {
        _paymentState.value = PaymentState.Loading
        viewModelScope.launch {
            try {
                val result = paymentRepository.getPaymentStatus(orderId)
                if (result is Resource.Success) {
                    _paymentState.value = PaymentState.Success(
                        status = result.data.status,
                        paymentMethod = result.data.paymentMethod,
                        paymentTime = result.data.paymentTime,
                        totalAmount = result.data.totalAmount,
                        paymentUrl = result.data.paymentUrl
                    )

                    // 如果支付状态为pending，每10秒自动刷新一次
                    if (result.data.status == "payment_pending") {
                        delay(10000)
                        getPaymentStatus(orderId)
                    }
                } else {
                    _paymentState.value = PaymentState.Error(
                        (result as Resource.Error).message
                    )
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "获取支付状态失败")
            }
        }
    }

    // 获取订单详情
    fun getOrderDetails(orderId: String) {
        _orderState.value = OrderState.Loading
        viewModelScope.launch {
            try {
                val result = orderRepository.getOrderDetails(orderId)
                if (result is Resource.Success) {
                    _orderState.value = OrderState.Success(result.data)
                } else {
                    _orderState.value = OrderState.Error(
                        (result as Resource.Error).message
                    )
                }
            } catch (e: Exception) {
                _orderState.value = OrderState.Error(e.message ?: "获取订单详情失败")
            }
        }
    }

    // 取消支付
    suspend fun cancelPayment(orderId: String) {
        try {
            paymentRepository.cancelPayment(orderId)
        } catch (e: Exception) {
            // 处理错误
        }
    }

    fun confirmPayment(orderId: String) = viewModelScope.launch {
        try {
            paymentRepository.confirmPayment(orderId)
            getPaymentStatus(orderId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 支付状态的密封类
    sealed class PaymentState {
        data object Loading : PaymentState()
        data class Success(
            val status: String,
            val paymentMethod: String,
            val paymentTime: Long?,
            val totalAmount: Double,
            val paymentUrl: String?
        ) : PaymentState()

        data class Error(val message: String) : PaymentState()
    }

    // 订单状态的密封类
    sealed class OrderState {
        data object Loading : OrderState()
        data class Success(val orderDetails: OrderDetails) : OrderState()
        data class Error(val message: String) : OrderState()
    }
}
