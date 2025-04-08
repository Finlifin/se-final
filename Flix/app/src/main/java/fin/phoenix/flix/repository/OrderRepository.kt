package fin.phoenix.flix.repository

import android.content.Context
import fin.phoenix.flix.api.CreateOrderRequest
import fin.phoenix.flix.api.OrderService
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.api.UpdateOrderStatusRequest
import fin.phoenix.flix.data.Order
import fin.phoenix.flix.data.OrderDetails
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository(context: Context) {
    private val orderService = RetrofitClient.createService(OrderService::class.java, context)
    
    /**
     * 创建订单
     */
    suspend fun createOrder(productId: String): Resource<Order> = withContext(Dispatchers.IO) {
        val request = CreateOrderRequest(product_id = productId)
        orderService.createOrder(request).toResource("创建订单失败")
    }
    
    /**
     * 获取订单列表
     */
    suspend fun getOrders(
        limit: Int = 10,
        offset: Int = 0,
        role: String = "buyer",
        status: String? = null
    ): Resource<List<Order>> = withContext(Dispatchers.IO) {
        val response = orderService.getOrders(limit, offset, role, status).toResource("获取订单列表失败")
        if (response is Resource.Success) {
            Resource.Success(response.data.orders)
        } else if (response is Resource.Error) {
            response
        } else {
            Resource.Error("获取订单列表失败")
        }
    }
    
    /**
     * 获取订单详情
     */
    suspend fun getOrderDetails(orderId: String): Resource<OrderDetails> = withContext(Dispatchers.IO) {
        orderService.getOrderDetails(orderId).toResource("获取订单详情失败")
    }

    /**
     * 更新订单状态
     */
    suspend fun updateOrderStatus(orderId: String, status: String): Resource<Order> = withContext(Dispatchers.IO) {
        val request = UpdateOrderStatusRequest(status = status)
        orderService.updateOrderStatus(orderId, request).toResource("更新订单状态失败")
    }
    
    /**
     * 取消订单
     */
    suspend fun cancelOrder(orderId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        orderService.cancelOrder(orderId).toResource("取消订单失败")
    }
}
