package fin.phoenix.flix.ui.myprofile

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.R
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.product.PaymentMethod
import fin.phoenix.flix.ui.product.PaymentMethodItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeScreen(
    navController: NavController, 
    userId: String
) {
    val viewModel: RechargeViewModel = viewModel()
    val rechargeState by viewModel.rechargeState.observeAsState()
    
    var amount by remember { mutableIntStateOf(0) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var isRecharging by remember { mutableStateOf(false) }
    var rechargeSuccess by remember { mutableStateOf(false) }
    
    val paymentMethods = listOf(
        PaymentMethod("alipay", "支付宝", R.drawable.alipay_square_),
        PaymentMethod("wechat", "微信支付", R.drawable.wechat_pay),
        PaymentMethod("card", "银行卡", R.drawable.union_pay)
    )
    
    var selectedPaymentMethod by remember { mutableStateOf(paymentMethods.first().id) }
    
    // 预设的充值金额
    val presetAmounts = listOf(10, 50, 100, 500, 1000)
    
    // 处理充值结果
    LaunchedEffect(rechargeState) {
        if (isRecharging) {
            when (rechargeState) {
                is RechargeViewModel.RechargeState.Success -> {
                    rechargeSuccess = true
                    isRecharging = false
                }
                is RechargeViewModel.RechargeState.Error -> {
                    isRecharging = false
                }
                else -> {}
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账户充值") },
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
            if (rechargeSuccess) {
                // 充值成功界面
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "充值成功",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Green
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "充值成功",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "您已成功充值 ¥$amount",
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                    ) {
                        Text("返回个人中心")
                    }
                }
            } else {
                // 充值表单界面
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "充值金额",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = if (amount > 0) amount.toString() else "",
                        onValueChange = { 
                            try {
                                if (it.isEmpty()) {
                                    amount = 0
                                    amountError = null
                                } else {
                                    val value = it.toInt()
                                    if (value > 0) {
                                        amount = value
                                        amountError = null
                                    } else {
                                        amountError = "请输入大于0的金额"
                                    }
                                }
                            } catch (e: NumberFormatException) {
                                amountError = "请输入有效的数字"
                            }
                        },
                        label = { Text("充值金额") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError != null,
                        supportingText = amountError?.let { { Text(it) } },
                        prefix = { Text("¥") }
                    )
                    
                    Text(
                        text = "推荐金额",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(presetAmounts) { preset ->
                            Card(
                                onClick = { amount = preset },
                                modifier = Modifier.padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (amount == preset) RoseRed else Color.White
                                )
                            ) {
                                Text(
                                    text = "¥$preset",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = if (amount == preset) Color.White else Color.Black
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "支付方式",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(paymentMethods) { method ->
                            PaymentMethodItem(
                                paymentMethod = method,
                                isSelected = selectedPaymentMethod == method.id,
                                onClick = { selectedPaymentMethod = method.id }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            if (amount <= 0) {
                                amountError = "请输入有效的充值金额"
                                return@Button
                            }
                            
                            isRecharging = true
                            viewModel.rechargeBalance(userId, amount)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                        enabled = amount > 0 && !isRecharging
                    ) {
                        if (isRecharging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("确认充值 ¥$amount")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmountCard(
    amount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RoseRed else Color.White
        )
    ) {
        Text(
            text = "¥$amount",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .width(64.dp),
            textAlign = TextAlign.Center,
            color = if (isSelected) Color.White else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
