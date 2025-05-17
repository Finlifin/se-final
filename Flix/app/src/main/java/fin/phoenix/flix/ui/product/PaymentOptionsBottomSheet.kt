package fin.phoenix.flix.ui.product

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.R
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentOptionsBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onProceedToPayment: (paymentMethod: String, deliveryMethod: String, address: String) -> Unit
) {
    val context = LocalContext.current

    val paymentMethods = listOf(
        PaymentMethod("alipay", "支付宝", R.drawable.alipay_square_),
        PaymentMethod("wechat", "微信支付", R.drawable.wechat_pay),
        PaymentMethod("card", "银行卡", R.drawable.union_pay),
        PaymentMethod("wallet", "钱包余额", R.drawable.wallet_svgrepo_com)
    )

    val deliveryMethods = product.availableDeliveryMethods.map { methodId ->
        when (methodId) {
            // "express", "pickup", "self_delivery", "courier"
            "express" -> DeliveryMethod(
                "express",
                "快递配送",
                R.drawable.home_delivery_truck_svgrepo_com,
                10.0f
            )

            "pickup" -> DeliveryMethod("pickup", "自提", R.drawable.walk_svgrepo_com, 0.0f)
            "self_delivery" -> DeliveryMethod(
                "self_delivery",
                "商家配送",
                R.drawable.fast_delivery_svgrepo_com,
                0.0f
            )
            "courier" -> DeliveryMethod(
                "courier",
                "骑手配送",
                R.drawable.walk_svgrepo_com,
                5.0f
            )

            else -> DeliveryMethod(methodId, methodId, R.drawable.ic_launcher_foreground, 0.0f)
        }
    }

    val selectedPaymentMethod = remember { mutableStateOf(paymentMethods.firstOrNull()?.id ?: "") }
    val selectedDeliveryMethod =
        remember { mutableStateOf(deliveryMethods.firstOrNull()?.id ?: "") }
    val address = remember { mutableStateOf("") }
    val selectedDeliveryFee =
        deliveryMethods.find { it.id == selectedDeliveryMethod.value }?.baseFee ?: 0.0f
    val totalAmount = product.price + selectedDeliveryFee

    ModalBottomSheet(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "确认订单",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = product.images.firstOrNull()?.let { imageUrl(it) }),
                    contentDescription = "商品图片",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "¥${product.price}", color = RoseRed, fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "配送方式",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(deliveryMethods) { method ->
                    DeliveryMethodItem(
                        deliveryMethod = method,
                        isSelected = selectedDeliveryMethod.value == method.id,
                        onClick = { selectedDeliveryMethod.value = method.id })
                }
            }

            if (selectedDeliveryMethod.value == "express" || selectedDeliveryMethod.value == "same_day") {
                OutlinedTextField(
                    value = address.value,
                    onValueChange = { address.value = it },
                    label = { Text("配送地址") },
                    placeholder = { Text("请输入详细地址") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = false,
                    maxLines = 3
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "支付方式",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(paymentMethods) { method ->
                    PaymentMethodItem(
                        paymentMethod = method,
                        isSelected = selectedPaymentMethod.value == method.id,
                        onClick = { selectedPaymentMethod.value = method.id })
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "费用明细",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "商品金额")
                    Text(text = "¥${product.price}")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "配送费用")
                    Text(text = "¥${selectedDeliveryFee}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "合计", fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "¥${totalAmount}", color = RoseRed, fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    if ((selectedDeliveryMethod.value == "express" || selectedDeliveryMethod.value == "same_day") && address.value.isBlank()) {
                        Toast.makeText(context, "请填写配送地址", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    onProceedToPayment(
                        selectedPaymentMethod.value, selectedDeliveryMethod.value, address.value
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
            ) {
                Text("确认支付 ¥${totalAmount}")
            }
        }
    }
}
