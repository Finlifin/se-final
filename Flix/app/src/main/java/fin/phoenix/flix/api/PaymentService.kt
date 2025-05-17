package fin.phoenix.flix.api

import com.google.gson.annotations.SerializedName
import fin.phoenix.flix.data.DeliveryMethod
import fin.phoenix.flix.data.PaymentDetails
import fin.phoenix.flix.data.PaymentMethod
import fin.phoenix.flix.data.PaymentStatus
import retrofit2.Response
import retrofit2.http.*
import java.io.Serial

/**
 * 支付API服务接口
 */
interface PaymentService {
    /**
     * 创建支付订单
     */
    @POST("payment/create")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<GenericApiResponse<PaymentDetails>>

    /**
     * 获取支付状态
     */
    @GET("payment/{orderId}/status")
    suspend fun getPaymentStatus(@Path("orderId") orderId: String): Response<GenericApiResponse<PaymentStatus>>

    /**
     * 获取支付方式列表
     */
    @GET("payment/methods")
    suspend fun getPaymentMethods(): Response<GenericApiResponse<List<PaymentMethod>>>

    /**
     * 获取配送方式列表
     */
    @GET("payment/delivery_methods")
    suspend fun getDeliveryMethods(@Query("product_id") productId: String): Response<GenericApiResponse<List<DeliveryMethod>>>

    /**
     * 计算配送费用
     */
    @POST("payment/calculate_delivery_fee")
    suspend fun calculateDeliveryFee(@Body request: CalculateDeliveryFeeRequest): Response<GenericApiResponse<DeliveryFeeData>>

    /**
     * 取消支付
     */
    @DELETE("payment/{orderId}")
    suspend fun cancelPayment(@Path("orderId") orderId: String): Response<GenericApiResponse<Boolean>>

    // 用于测试
    @POST("payment/callback")
    suspend fun confirmPayment(@Body request: PaymentCallbackRequest): Response<GenericApiResponse<Unit>>
}

data class PaymentCallbackRequest(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("payment_status") val paymentStatus: String = "success",
    @SerializedName("transaction_id") val transactionId: String
)
/**
 * 配送费用数据
 */
data class DeliveryFeeData(
    val delivery_fee: Double
)

/**
 * 创建支付请求
 */
data class CreatePaymentRequest(
    val order_id: String,
    val payment_method: String,
    val delivery_method: String,
    val delivery_address: String
)

/**
 * 计算配送费用请求
 */
data class CalculateDeliveryFeeRequest(
    val product_id: String,
    val delivery_method: String,
    val address: String
)
