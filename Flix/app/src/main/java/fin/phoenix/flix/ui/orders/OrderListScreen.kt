package fin.phoenix.flix.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.Order
import fin.phoenix.flix.data.OrderStatus
import fin.phoenix.flix.ui.colors.RoseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(navController: NavController) {
    val viewModel: OrderViewModel = viewModel()
    val ordersState by viewModel.ordersState.collectAsState()

    // 角色选择（买家/卖家）
    val roleOptions = listOf("买家", "卖家")
    val selectedRole = remember { mutableStateOf(roleOptions[0]) }

    // 订单状态筛选
    val statusOptions = listOf(
        "全部", "待处理", "等待支付", "已支付", "已发货", "已完成", "已取消", "已退款"
    )
    val selectedStatus = remember { mutableStateOf(statusOptions[0]) }

    // 加载订单
    LaunchedEffect(selectedRole.value, selectedStatus.value) {
        val role = if (selectedRole.value == "买家") "buyer" else "seller"
        val status = when (selectedStatus.value) {
            "待处理" -> "pending"
            "等待支付" -> "payment_pending"
            "已支付" -> "paid"
            "已发货" -> "shipping"
            "已完成" -> "completed"
            "已取消" -> "cancelled"
            "已退款" -> "refunded"
            else -> null
        }
        viewModel.loadOrders(role = role, status = status)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("我的订单") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            })
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 角色选择器
            RoleSelector(
                options = roleOptions,
                selectedOption = selectedRole.value,
                onOptionSelected = { selectedRole.value = it })

            // // 状态筛选器
            // StatusFilterChips(
            //     options = statusOptions,
            //     selectedOption = selectedStatus.value,
            //     onOptionSelected = { selectedStatus.value = it }
            // )

            // 订单列表
            when (ordersState) {
                is OrderViewModel.OrdersState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoseRed)
                    }
                }

                is OrderViewModel.OrdersState.Error -> {
                    val errorMessage = (ordersState as OrderViewModel.OrdersState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "加载订单失败",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage, color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val role =
                                        if (selectedRole.value == "买家") "buyer" else "seller"
                                    val status = when (selectedStatus.value) {
                                        "待处理" -> "pending"
                                        "等待支付" -> "payment_pending"
                                        "已支付" -> "paid"
                                        "已发货" -> "shipping"
                                        "已完成" -> "completed"
                                        "已取消" -> "cancelled"
                                        "已退款" -> "refunded"
                                        else -> null
                                    }
                                    viewModel.loadOrders(role = role, status = status)
                                }, colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }

                is OrderViewModel.OrdersState.Success -> {
                    val orders = (ordersState as OrderViewModel.OrdersState.Success).orders.filter {
                        it.orderType == "product"
                    }

                    if (orders.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.ShoppingBag,
                            message = "暂无订单",
                            subMessage = "您当前没有${selectedStatus.value}的订单"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(
                                items = orders, key = { order -> order.orderId }) { order ->
                                OrderItem(
                                    order = order,
                                    isSellerMode = selectedRole.value == "卖家",
                                    onClick = { navController.navigate("/orders/${order.orderId}") },
                                    onGotoPayment = {
                                        navController.navigate("/payment/confirm/${order.orderId}")
                                    },
                                    onCancelOrder = {
                                        viewModel.cancelOrder(order.orderId)
                                    })
                            }

                            // 添加底部空间以防止底部导航栏遮挡
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption

            OutlinedButton(
                onClick = { onOptionSelected(option) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) RoseRed.copy(alpha = 0.1f) else Color.Transparent,
                    contentColor = if (isSelected) RoseRed else Color.Gray
                ),
                border = BorderStroke(
                    width = 1.dp, color = if (isSelected) RoseRed else Color.Gray
                )
            ) {
                Text(option)
            }

            if (option != options.last()) {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
fun StatusFilterChips(
    options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(options) { option ->
            val isSelected = option == selectedOption

            FilterChip(
                selected = isSelected,
                onClick = { onOptionSelected(option) },
                label = { Text(option) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoseRed.copy(alpha = 0.1f),
                    selectedLabelColor = RoseRed,
                    selectedLeadingIconColor = RoseRed
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = RoseRed,
                    borderWidth = 1.dp,
                    enabled = TODO(),
                    selected = TODO(),
                    borderColor = TODO(),
                    disabledBorderColor = TODO(),
                    disabledSelectedBorderColor = TODO(),
                    selectedBorderWidth = TODO()
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun OrderItem(
    order: Order,
    isSellerMode: Boolean,
    onClick: () -> Unit,
    onGotoPayment: () -> Unit = {},
    onCancelOrder: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 订单ID和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "订单号: ${order.orderId.take(8)}...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                StatusChip(
                    status = order.status
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 占位图片和订单基本信息
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                // 占位图片，实际应用中应该是产品图片
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "商品", color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isSellerMode) "买家订单" else "卖家商品",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatOrderTime(order.orderTime),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "¥${order.price}", fontWeight = FontWeight.Bold, color = RoseRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                // 根据订单状态和角色显示不同的操作按钮
                when (order.status) {
                    OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING -> {
                        if (!isSellerMode) {
                            OutlinedButton(
                                onClick = onGotoPayment,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RoseRed
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("去支付")
                            }
                        }

                        OutlinedButton(
                            onClick = onCancelOrder,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Gray
                            )
                        ) {
                            Text("取消订单")
                        }
                    }

                    OrderStatus.PAID -> {
                        if (isSellerMode) {
                            Button(
                                onClick = { /* 发货 */ }, colors = ButtonDefaults.buttonColors(
                                    containerColor = RoseRed
                                )
                            ) {
                                Text("发货")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { /* 提醒卖家发货 */ },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RoseRed
                                )
                            ) {
                                Text("提醒发货")
                            }
                        }
                    }

                    OrderStatus.SHIPPING -> {
                        if (!isSellerMode) {
                            Button(
                                onClick = { /* 确认收货 */ }, colors = ButtonDefaults.buttonColors(
                                    containerColor = RoseRed
                                )
                            ) {
                                Text("确认收货")
                            }
                        }
                    }

                    OrderStatus.COMPLETED -> {
                        if (!isSellerMode) {
                            OutlinedButton(
                                onClick = { /* 查看详情 */ },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RoseRed
                                )
                            ) {
                                Text("再次购买")
                            }
                        }
                    }

                    else -> {
                        // 对于已取消或已退款的订单，不显示操作按钮
                    }
                }

                // 所有状态都显示查看详情按钮
                OutlinedButton(
                    onClick = onClick, colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    ), modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("查看详情")
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: OrderStatus) {
    val (backgroundColor, textColor, statusText) = when (status) {
        OrderStatus.PENDING -> Triple(Color(0xFFE6F7FF), Color(0xFF1890FF), "待处理")
        OrderStatus.PAYMENT_PENDING -> Triple(Color(0xFFFFF7E6), Color(0xFFFA8C16), "等待支付")
        OrderStatus.PAID -> Triple(Color(0xFFE6FFFB), Color(0xFF13C2C2), "已支付")
        OrderStatus.SHIPPING -> Triple(Color(0xFFF6FFED), Color(0xFF52C41A), "已发货")
        OrderStatus.COMPLETED -> Triple(Color(0xFFE6F7FF), Color(0xFF1890FF), "已完成")
        OrderStatus.CANCELLED -> Triple(Color(0xFFF5F5F5), Color.Gray, "已取消")
        OrderStatus.REFUNDED -> Triple(Color(0xFFFFF1F0), Color(0xFFF5222D), "已退款")
    }

    Surface(
        shape = RoundedCornerShape(4.dp), color = backgroundColor
    ) {
        Text(
            text = statusText,
            color = textColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector, message: String, subMessage: String? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )

            if (subMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subMessage, fontSize = 14.sp, color = Color.Gray
                )
            }
        }
    }
}

// 格式化订单时间
fun formatOrderTime(timestamp: Long): String {
    val date = Date(timestamp * 1000) // 转换为毫秒
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return format.format(date)
}
