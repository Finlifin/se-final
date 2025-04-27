package fin.phoenix.flix.repository

import android.content.Context
import fin.phoenix.flix.api.CalculateDeliveryFeeRequest
import fin.phoenix.flix.api.CreatePaymentRequest
import fin.phoenix.flix.api.PaymentCallbackRequest
import fin.phoenix.flix.api.PaymentService
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.data.DeliveryMethod
import fin.phoenix.flix.data.PaymentDetails
import fin.phoenix.flix.data.PaymentMethod
import fin.phoenix.flix.data.PaymentStatus
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentRepository(context: Context) {
    private val paymentService = RetrofitClient.createService(PaymentService::class.java, context)
    
    /**
     * 创建支付订单
     */
    suspend fun setupPayment(
        orderId: String,
        paymentMethod: String,
        deliveryMethod: String,
        deliveryAddress: String
    ): Resource<PaymentDetails> = withContext(Dispatchers.IO) {
        val request = CreatePaymentRequest(
            order_id = orderId,
            payment_method = paymentMethod,
            delivery_method = deliveryMethod,
            delivery_address = deliveryAddress
        )
        
        paymentService.createPayment(request).toResource("设置支付信息失败")
    }
    
    /**
     * 获取支付状态
     */
    suspend fun getPaymentStatus(orderId: String): Resource<PaymentStatus> {
        return paymentService.getPaymentStatus(orderId).toResource("获取支付状态失败")
    }

    suspend fun confirmPayment(orderId: String): Resource<Unit> {
        return paymentService.confirmPayment(PaymentCallbackRequest(orderId, "success", "test transaction")).toResource("无法模拟确认支付")
    }
    
    /**
     * 获取支付方式列表
     */
    suspend fun getPaymentMethods(): Resource<List<PaymentMethod>> = withContext(Dispatchers.IO) {
        paymentService.getPaymentMethods().toResource("获取支付方式列表失败")
    }
    
    /**
     * 获取配送方式列表
     */
    suspend fun getDeliveryMethods(productId: String): Resource<List<DeliveryMethod>> = withContext(Dispatchers.IO) {
        paymentService.getDeliveryMethods(productId).toResource("获取配送方式列表失败")
    }
    
    /**
     * 计算配送费用
     */
    suspend fun calculateDeliveryFee(
        productId: String,
        deliveryMethod: String,
        address: String
    ): Resource<Double> = withContext(Dispatchers.IO) {
        val request = CalculateDeliveryFeeRequest(
            product_id = productId,
            delivery_method = deliveryMethod,
            address = address
        )
        
        val response = paymentService.calculateDeliveryFee(request).toResource("计算配送费用失败")
        if (response is Resource.Success) {
            Resource.Success(response.data.delivery_fee)
        } else if (response is Resource.Error) {
            response
        } else {
            Resource.Error("计算配送费用失败")
        }
    }
    
    /**
     * 取消支付
     */
    suspend fun cancelPayment(orderId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        paymentService.cancelPayment(orderId).toResource("取消支付失败")
    }
}
