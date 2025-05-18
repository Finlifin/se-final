package fin.phoenix.flix.ui.payment

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.OrderDetails
import fin.phoenix.flix.ui.colors.RoseRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmScreen(navController: NavController, orderId: String) {
    val viewModel: PaymentViewModel = viewModel()
    val paymentState by viewModel.paymentState.collectAsState()
    val orderState by viewModel.orderState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 页面加载时获取支付信息
    LaunchedEffect(orderId) {
        viewModel.getPaymentStatus(orderId)
        viewModel.getOrderDetails(orderId)
    }

    // 当支付状态为PAID时，自动跳转到订单详情页面
    LaunchedEffect(paymentState) {
        if (paymentState is PaymentViewModel.PaymentState.Success) {
            val status = (paymentState as PaymentViewModel.PaymentState.Success).status
            if (status == "paid") {
                // 延迟一下，让用户看到支付成功的界面
                delay(1500)
                navController.navigate("/orders/$orderId") {
                    popUpTo("/payment/confirm/$orderId") { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("支付确认") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            })
        }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                paymentState is PaymentViewModel.PaymentState.Loading || orderState is PaymentViewModel.OrderState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), color = RoseRed
                    )
                }

                paymentState is PaymentViewModel.PaymentState.Error -> {
                    val errorMessage = (paymentState as PaymentViewModel.PaymentState.Error).message
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "获取支付信息失败",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage, color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.getPaymentStatus(orderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                        ) {
                            Text("重试")
                        }
                    }
                }

                orderState is PaymentViewModel.OrderState.Error -> {
                    val errorMessage = (orderState as PaymentViewModel.OrderState.Error).message
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
                            text = errorMessage, color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.getOrderDetails(orderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                        ) {
                            Text("重试")
                        }
                    }
                }

                paymentState is PaymentViewModel.PaymentState.Success && orderState is PaymentViewModel.OrderState.Success -> {
                    val paymentDetails = (paymentState as PaymentViewModel.PaymentState.Success)
                    val orderDetails =
                        (orderState as PaymentViewModel.OrderState.Success).orderDetails

                    if (paymentDetails.status == "payment_pending") {
                        // 显示支付网页
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 显示订单信息
                            OrderSummary(orderDetails)

                            Spacer(modifier = Modifier.height(16.dp))

                            // 显示支付网页
//                            AndroidView(
//                                factory = { context ->
//                                    WebView(context).apply {
//                                        webViewClient = WebViewClient()
//                                        settings.javaScriptEnabled = true
//                                        paymentDetails.paymentUrl?.let { loadUrl(it) }
//                                    }
//                                }, modifier = Modifier
//                                    .fillMaxWidth()
//                                    .weight(1f)
//                            )

                            // 底部按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.cancelPayment(orderId)
                                            navController.popBackStack()
                                        }
                                    }, modifier = Modifier.weight(1f)
                                ) {
                                    Text("取消支付")
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Button(
                                    onClick = { viewModel.getPaymentStatus(orderId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                                ) {
                                    Text("刷新状态")
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 测试需要，模拟支付
                                Button(
                                    onClick = { viewModel.confirmPayment(orderId) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                                ) {
                                    Text("模拟支付")
                                }
                            }

                        }
                    } else if (paymentDetails.status == "paid") {
                        // 显示支付成功界面
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "支付成功",
                                modifier = Modifier.size(80.dp),
                                tint = Color.Green
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "支付成功", fontSize = 24.sp, fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "订单已支付，请等待卖家发货", color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    navController.navigate("/orders/$orderId") {
                                        popUpTo("/payment/confirm/$orderId") { inclusive = true }
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                            ) {
                                Text("查看订单详情")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSummary(orderDetails: OrderDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "订单摘要", fontSize = 18.sp, fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "商品")
                Text(
                    text = orderDetails.product.title, fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "商品金额")
                Text(
                    text = "¥${orderDetails.product.price}", fontWeight = FontWeight.SemiBold
                )
            }

            if (orderDetails.deliveryFee != null && orderDetails.deliveryFee > 0) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "配送费用")
                    Text(
                        text = "¥${orderDetails.deliveryFee}", fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "总计", fontWeight = FontWeight.Bold
                )
                Text(
                    text = "¥${orderDetails.price + (orderDetails.deliveryFee ?: 0.0)}",
                    fontWeight = FontWeight.Bold,
                    color = RoseRed
                )
            }
        }
    }
}
