package fin.phoenix.flix.api

import fin.phoenix.flix.data.Order
import fin.phoenix.flix.data.OrderDetails
import retrofit2.Response
import retrofit2.http.*

/**
 * 订单API服务接口
 */
interface OrderService {
    /**
     * 获取订单列表
     */
    @GET("orders")
    suspend fun getOrders(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("role") role: String = "buyer",
        @Query("status") status: String? = null
    ): Response<GenericApiResponse<OrderListData>>

    /**
     * 获取订单详情
     */
    @GET("orders/{id}")
    suspend fun getOrderDetails(@Path("id") orderId: String): Response<GenericApiResponse<OrderDetails>>

    /**
     * 创建订单
     */
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<GenericApiResponse<Order>>

    /**
     * 更新订单状态
     */
    @PUT("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String, 
        @Body request: UpdateOrderStatusRequest
    ): Response<GenericApiResponse<Order>>

    /**
     * 取消订单
     */
    @DELETE("orders/{id}")
    suspend fun cancelOrder(@Path("id") orderId: String): Response<GenericApiResponse<Order>>
}

/**
 * 订单列表数据
 */
data class OrderListData(
    val orders: List<Order> = emptyList(),
    val pagination: PaginationInfo = PaginationInfo()
)

/**
 * 分页信息
 */
data class PaginationInfo(
    val totalCount: Int = 0,
    val limit: Int = 10,
    val offset: Int = 0
)

/**
 * 创建订单请求
 */
data class CreateOrderRequest(
    val product_id: String
)

/**
 * 更新订单状态请求
 */
data class UpdateOrderStatusRequest(
    val status: String
)
