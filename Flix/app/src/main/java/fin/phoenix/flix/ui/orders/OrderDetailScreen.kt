package fin.phoenix.flix.ui.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.data.OrderDetails
import fin.phoenix.flix.data.OrderStatus
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(navController: NavController, orderId: String) {
    val viewModel: OrderViewModel = viewModel()
    val orderState by viewModel.orderDetailsState.collectAsState()
    
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetails(orderId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("订单详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (orderState) {
                is OrderViewModel.OrderDetailsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RoseRed
                    )
                }
                
                is OrderViewModel.OrderDetailsState.Error -> {
                    val errorMessage = (orderState as OrderViewModel.OrderDetailsState.Error).message
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "获取订单信息失败",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadOrderDetails(orderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                        ) {
                            Text("重试")
                        }
                    }
                }
                
                is OrderViewModel.OrderDetailsState.Success -> {
                    val orderDetails = (orderState as OrderViewModel.OrderDetailsState.Success).orderDetails
                    OrderDetailsContent(
                        orderDetails = orderDetails,
                        navController = navController,
                        onUpdateStatus = { newStatus -> 
                            viewModel.updateOrderStatus(orderId, newStatus) 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailsContent(
    orderDetails: OrderDetails,
    navController: NavController,
    onUpdateStatus: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 订单状态卡片
        OrderStatusCard(orderDetails)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 商品信息卡片
        ProductInfoCard(orderDetails, navController)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 订单信息卡片
        OrderInfoCard(orderDetails)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 配送信息卡片
        if (orderDetails.deliveryMethod != null) {
            DeliveryInfoCard(orderDetails)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 支付信息卡片
        if (orderDetails.paymentMethod != null) {
            PaymentInfoCard(orderDetails)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 操作按钮
        OrderActionButtons(orderDetails, onUpdateStatus)
    }
}

@Composable
fun OrderStatusCard(orderDetails: OrderDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "订单状态",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (orderDetails.status) {
                    OrderStatus.PENDING -> "待处理"
                    OrderStatus.PAYMENT_PENDING -> "等待支付"
                    OrderStatus.PAID -> "已支付，等待发货"
                    OrderStatus.SHIPPING -> "已发货，等待收货"
                    OrderStatus.COMPLETED -> "已完成"
                    OrderStatus.CANCELLED -> "已取消"
                    OrderStatus.REFUNDED -> "已退款"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when (orderDetails.status) {
                    OrderStatus.COMPLETED -> Color.Green
                    OrderStatus.CANCELLED, OrderStatus.REFUNDED -> Color.Red
                    else -> RoseRed
                }
            )
        }
    }
}

@Composable
fun ProductInfoCard(orderDetails: OrderDetails, navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "商品信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 商品图片
                Image(
                    painter = rememberAsyncImagePainter(
                        model = imageUrl(orderDetails.product.image)
                    ),
                    contentDescription = "商品图片",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 商品信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = orderDetails.product.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "¥${orderDetails.product.price}",
                        color = RoseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 查看商品按钮
                IconButton(onClick = { navController.navigate("/product/${orderDetails.product.id}") }) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "查看商品",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun OrderInfoCard(orderDetails: OrderDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "订单信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("订单编号", orderDetails.orderId)
            InfoRow("下单时间", formatTimestamp(orderDetails.orderTime))
            InfoRow("买家", orderDetails.buyer.userName)
            InfoRow("卖家", orderDetails.seller.userName)
            InfoRow("商品金额", "¥${orderDetails.product.price}")
            
            if (orderDetails.deliveryFee != null && orderDetails.deliveryFee > 0) {
                InfoRow("配送费用", "¥${orderDetails.deliveryFee}")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoRow(
                "订单总额", 
                "¥${orderDetails.price}",
                isBold = true,
                valueColor = RoseRed
            )
        }
    }
}

@Composable
fun DeliveryInfoCard(orderDetails: OrderDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "配送信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("配送方式", when(orderDetails.deliveryMethod) {
                "express" -> "快递配送"
                "pickup" -> "自提"
                "same_day" -> "当日达"
                else -> orderDetails.deliveryMethod ?: ""
            })
            
            if (orderDetails.deliveryAddress != null) {
                InfoRow("配送地址", orderDetails.deliveryAddress)
            }
            
            if (orderDetails.deliveryTime != null) {
                InfoRow("发货时间", formatTimestamp(orderDetails.deliveryTime))
            }
        }
    }
}

@Composable
fun PaymentInfoCard(orderDetails: OrderDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "支付信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow("支付方式", when(orderDetails.paymentMethod) {
                "alipay" -> "支付宝"
                "wechat" -> "微信支付"
                "card" -> "银行卡"
                "wallet" -> "钱包余额"
                else -> orderDetails.paymentMethod ?: ""
            })
            
            if (orderDetails.paymentTime != null) {
                InfoRow("支付时间", formatTimestamp(orderDetails.paymentTime))
            }
        }
    }
}

@Composable
fun OrderActionButtons(
    orderDetails: OrderDetails,
    onUpdateStatus: (String) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    val actionType = remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (orderDetails.status) {
            OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING -> {
                // 买家可以取消订单
                OutlinedButton(
                    onClick = { 
                        actionType.value = "cancel"
                        showDialog.value = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消订单")
                }
            }
            
            OrderStatus.PAID -> {
                // 卖家可以发货
                if (isSeller(orderDetails)) {
                    Button(
                        onClick = { 
                            actionType.value = "ship"
                            showDialog.value = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                    ) {
                        Text("确认发货")
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            actionType.value = "cancel"
                            showDialog.value = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消订单")
                    }
                }
            }
            
            OrderStatus.SHIPPING -> {
                // 买家可以确认收货
                if (!isSeller(orderDetails)) {
                    Button(
                        onClick = { 
                            actionType.value = "complete"
                            showDialog.value = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                    ) {
                        Text("确认收货")
                    }
                }
                
                // 卖家可以退款
                if (isSeller(orderDetails)) {
                    OutlinedButton(
                        onClick = { 
                            actionType.value = "refund"
                            showDialog.value = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("申请退款")
                    }
                }
            }
            
            OrderStatus.COMPLETED -> {
                // 暂时没有操作
            }
            
            OrderStatus.CANCELLED, OrderStatus.REFUNDED -> {
                // 暂时没有操作
            }
        }
    }
    
    // 确认对话框
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { 
                Text(
                    when(actionType.value) {
                        "cancel" -> "取消订单"
                        "ship" -> "确认发货"
                        "complete" -> "确认收货"
                        "refund" -> "申请退款"
                        else -> "操作确认"
                    }
                ) 
            },
            text = { 
                Text(
                    when(actionType.value) {
                        "cancel" -> "确定要取消此订单吗？此操作无法撤销。"
                        "ship" -> "确认已发货？买家将收到发货通知。"
                        "complete" -> "确认已收到商品？确认后订单将完成。"
                        "refund" -> "确定要为此订单申请退款吗？"
                        else -> "确定要执行此操作吗？"
                    }
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newStatus = when(actionType.value) {
                            "cancel" -> "cancelled"
                            "ship" -> "shipping"
                            "complete" -> "completed"
                            "refund" -> "refunded"
                            else -> ""
                        }
                        if (newStatus.isNotEmpty()) {
                            onUpdateStatus(newStatus)
                        }
                        showDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun InfoRow(
    label: String, 
    value: String, 
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray
        )
        Text(
            text = value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

// 判断当前用户是否为卖家（这里需要根据你的实际逻辑实现）
fun isSeller(orderDetails: OrderDetails): Boolean {
    // 与当前登录用户ID比较
    val currentUserId = "current_user_id" // 这里应该从SessionManager或类似地方获取
    return orderDetails.seller.uid == currentUserId
}

// 格式化时间戳
fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""
    val date = Date(timestamp * 1000) // 转换为毫秒
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return format.format(date)
}
