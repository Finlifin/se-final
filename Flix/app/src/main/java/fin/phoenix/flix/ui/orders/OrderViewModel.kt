package fin.phoenix.flix.ui.orders

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Order
import fin.phoenix.flix.data.OrderDetails
import fin.phoenix.flix.repository.OrderRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val orderRepository = OrderRepository(application.applicationContext)

    // 订单列表状态
    private val _ordersState = MutableStateFlow<OrdersState>(OrdersState.Loading)
    val ordersState: StateFlow<OrdersState> = _ordersState
    
    // 订单详情状态
    private val _orderDetailsState = MutableStateFlow<OrderDetailsState>(OrderDetailsState.Loading)
    val orderDetailsState: StateFlow<OrderDetailsState> = _orderDetailsState
    
    // 加载订单列表
    fun loadOrders(
        limit: Int = 10,
        offset: Int = 0,
        role: String = "buyer",
        status: String? = null
    ) {
        _ordersState.value = OrdersState.Loading
        viewModelScope.launch {
            try {
                val result = orderRepository.getOrders(limit, offset, role, status)
                if (result is Resource.Success) {
                    _ordersState.value = OrdersState.Success(result.data)
                } else {
                    _ordersState.value = OrdersState.Error(
                        (result as Resource.Error).message ?: "获取订单列表失败"
                    )
                }
            } catch (e: Exception) {
                _ordersState.value = OrdersState.Error(e.message ?: "获取订单列表失败")
            }
        }
    }
    
    // 加载订单详情
    fun loadOrderDetails(orderId: String) {
        _orderDetailsState.value = OrderDetailsState.Loading
        viewModelScope.launch {
            try {
                val result = orderRepository.getOrderDetails(orderId)
                if (result is Resource.Success) {
                    Log.d("OrderViewModel", "Order details loaded successfully: ${result.data}")
                    _orderDetailsState.value = OrderDetailsState.Success(result.data)
                } else {
                    _orderDetailsState.value = OrderDetailsState.Error(
                        (result as Resource.Error).message ?: "获取订单详情失败"
                    )
                }
            } catch (e: Exception) {
                _orderDetailsState.value = OrderDetailsState.Error(e.message ?: "获取订单详情失败")
            }
        }
    }
    
    // 更新订单状态
    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try {
                val result = orderRepository.updateOrderStatus(orderId, status)
                if (result is Resource.Success) {
                    // 更新成功后重新加载订单详情
                    loadOrderDetails(orderId)
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    // 取消订单
    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                val result = orderRepository.cancelOrder(orderId)
                if (result is Resource.Success) {
                    // 取消成功后重新加载订单列表
                    loadOrders()
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error cancelling order: ${e.message}")
            }
        }
    }
    
    // 订单列表状态密封类
    sealed class OrdersState {
        object Loading : OrdersState()
        data class Success(val orders: List<Order>) : OrdersState()
        data class Error(val message: String) : OrdersState()
    }
    
    // 订单详情状态密封类
    sealed class OrderDetailsState {
        object Loading : OrderDetailsState()
        data class Success(val orderDetails: OrderDetails) : OrderDetailsState()
        data class Error(val message: String) : OrderDetailsState()
    }
}
