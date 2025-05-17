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
    private val profileRepository = ProfileRepository(context)
    private val productRepository = ProductRepository(context)

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
        limit: Int = 10, offset: Int = 0, role: String = "buyer", status: String? = null
    ): Resource<List<Order>> = withContext(Dispatchers.IO) {
        val response =
            orderService.getOrders(limit, offset, role, status).toResource("获取订单列表失败")
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
    suspend fun getOrderDetails(orderId: String): Resource<OrderDetails> =
        withContext(Dispatchers.IO) {
            var result = orderService.getOrderDetails(orderId).toResource("获取订单详情失败")
            when (result) {
                is Resource.Success -> {
                    var error = ""
                    when (val result0 = profileRepository.getUserAbstract(result.data.sellerId)) {
                        is Resource.Success -> {
                            result.data.seller = result0.data
                        }

                        else -> {
                            error += "获取卖家信息失败"
                        }
                    }
                    when (val result1 = productRepository.getProductById(result.data.productId)) {
                        is Resource.Success -> {
                            result.data.product = result1.data
                        }

                        else -> {
                            error += ", 获取商品信息失败"
                        }
                    }
                    when (val result2 = profileRepository.getUserAbstract(result.data.buyerId)) {
                        is Resource.Success -> {
                            result.data.buyer = result2.data
                        }

                        else -> {
                            error += ", 获取买家信息失败"
                        }
                    }
                    if (error.isNotEmpty()) {
                        result = Resource.Error(error)
                    }
                }

                is Resource.Loading -> {}
                is Resource.Error -> {
                    result = Resource.Error("获取订单详情失败")
                }
            }

            result
        }

    /**
     * 更新订单状态
     */
    suspend fun updateOrderStatus(orderId: String, status: String): Resource<Order> =
        withContext(Dispatchers.IO) {
            val request = UpdateOrderStatusRequest(status = status)
            orderService.updateOrderStatus(orderId, request).toResource("更新订单状态失败")
        }

    /**
     * 取消订单
     */
    suspend fun cancelOrder(orderId: String): Resource<Order> = withContext(Dispatchers.IO) {
        orderService.cancelOrder(orderId).toResource("取消订单失败")
    }
}
