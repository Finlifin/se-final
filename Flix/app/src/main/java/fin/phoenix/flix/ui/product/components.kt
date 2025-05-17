package fin.phoenix.flix.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl


@Composable
fun PaymentMethodItem(
    paymentMethod: PaymentMethod, isSelected: Boolean, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFFEEEE) else Color(0xFFF5F5F5),
        border = if (isSelected) BorderStroke(1.dp, RoseRed) else null,
        modifier = Modifier.width(90.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = paymentMethod.iconResId),
                contentDescription = paymentMethod.name,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = paymentMethod.name,
                fontSize = 12.sp,
                color = if (isSelected) RoseRed else Color.DarkGray
            )
        }
    }
}

@Composable
fun DeliveryMethodItem(
    deliveryMethod: DeliveryMethod, isSelected: Boolean, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFFEEEE) else Color(0xFFF5F5F5),
        border = if (isSelected) BorderStroke(1.dp, RoseRed) else null,
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = deliveryMethod.iconResId),
                contentDescription = deliveryMethod.name,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = deliveryMethod.name,
                fontSize = 12.sp,
                color = if (isSelected) RoseRed else Color.DarkGray
            )

            Text(
                text = if (deliveryMethod.baseFee > 0) "¥${deliveryMethod.baseFee}" else "免费",
                fontSize = 10.sp,
                color = if (deliveryMethod.baseFee > 0) RoseRed else Color.Green
            )
        }
    }
}

@Composable
fun BottomActionBar(
    isFavorited: Boolean,
    isMyProduct: Boolean,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onContactClick: () -> Unit,
    onBuyClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 收藏按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorited) "取消收藏" else "收藏",
                        tint = if (isFavorited) RoseRed else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))


            if (!isMyProduct) { // 联系卖家按钮
                OutlinedButton(
                    onClick = onContactClick,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, RoseRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("联系卖家")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 立即购买按钮
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("立即购买")
                }
            } else {
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, RoseRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑商品")
                }
            }

        }
    }
}

@Composable
fun ProductDetailTopBar(
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    seller: UserAbstract? = null,
    onSellerClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回"
                )
            }

            // 卖家信息部分
            if (seller != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { onSellerClick?.invoke() })
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painter = rememberAsyncImagePainter(model = seller.avatarUrl?.let {
                        imageUrl(
                            it
                        )
                    } ?: "https://randomuser.me/api/portraits/lego/1.jpg"),
                        contentDescription = "卖家头像",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = seller.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share, contentDescription = "分享"
                )
            }
        }
    }
}

@Composable
fun ProductInfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp), color = Color(0xFFF5F5F5)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}

data class PaymentMethod(
    val id: String, val name: String, val iconResId: Int
)

data class DeliveryMethod(
    val id: String, val name: String, val iconResId: Int, val baseFee: Float
)
