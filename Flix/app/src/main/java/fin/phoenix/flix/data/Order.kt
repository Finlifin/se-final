package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName

/**
 * Order status enum
 * Represents the current status of an order
 */
enum class OrderStatus {
    @SerializedName("pending") PENDING,                // 初始状态，订单已创建但未发起支付
    @SerializedName("payment_pending") PAYMENT_PENDING, // 已选择支付方式，等待支付
    @SerializedName("paid") PAID,                     // 已支付，等待卖家发货
    @SerializedName("shipping") SHIPPING,             // 卖家已发货，等待买家确认收货
    @SerializedName("completed") COMPLETED,           // 买家已确认收货，订单完成
    @SerializedName("cancelled") CANCELLED,           // 订单已取消
    @SerializedName("refunded") REFUNDED              // 订单已退款
}

/**
 * Order data model class
 * Represents an order in the second-hand trading platform
 */
data class Order(
    @SerializedName("order_id") val orderId: String, // 订单ID，唯一标识
    @SerializedName("buyer_id") val buyerId: String, // 买家ID
    @SerializedName("seller_id") val sellerId: String, // 卖家ID
    @SerializedName("product_id") val productId: String, // 商品ID
    @SerializedName("order_time") val orderTime: Long, // 下单时间戳
    @SerializedName("price") val price: Double, // 订单金额
    @SerializedName("status") val status: OrderStatus, // 订单状态
    @SerializedName("delivery_method") val deliveryMethod: String? = null, // 配送方式
    @SerializedName("delivery_address") val deliveryAddress: String? = null, // 配送地址
    @SerializedName("delivery_time") val deliveryTime: Long? = null, // 发货时间
    @SerializedName("delivery_fee") val deliveryFee: Double? = null, // 配送费用
    @SerializedName("payment_method") val paymentMethod: String? = null, // 支付方式
    @SerializedName("payment_time") val paymentTime: Long? = null // 支付时间
)

/**
 * OrderDetails data model class
 * Represents detailed order information including related product and user data
 */
data class OrderDetails(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("buyer") val buyer: UserAbstract,
    @SerializedName("seller") val seller: UserAbstract,
    @SerializedName("product") val product: ProductAbstract,
    @SerializedName("order_time") val orderTime: Long,
    @SerializedName("price") val price: Double,
    @SerializedName("status") val status: OrderStatus,
    @SerializedName("delivery_method") val deliveryMethod: String? = null,
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("delivery_time") val deliveryTime: Long? = null,
    @SerializedName("delivery_fee") val deliveryFee: Double? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_time") val paymentTime: Long? = null
)

/**
 * Payment related data classes
 */
data class PaymentDetails(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_url") val paymentUrl: String
)

data class PaymentStatus(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("status") val status: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_time") val paymentTime: Long?,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("payment_url") val paymentUrl: String? = null
)

data class PaymentMethod(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String
)

data class DeliveryMethod(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("base_fee") val baseFee: Double
)
