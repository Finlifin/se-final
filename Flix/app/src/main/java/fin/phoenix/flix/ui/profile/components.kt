package fin.phoenix.flix.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.Seller
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.home.ProductCard
import fin.phoenix.flix.util.imageUrl

@Composable
fun UserProfileHeader(
    user: Seller, onMessageClick: () -> Unit, onFollowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // User info row
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar
            Image(painter = rememberAsyncImagePainter(model = user.avatarUrl?.let { imageUrl(it) }
                ?: "https://randomuser.me/api/portraits/lego/1.jpg"),
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop)

            Spacer(modifier = Modifier.width(16.dp))

            // User stats
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.userName, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "已发布: ${user.publishedProductIds.size} · 已售出: ${user.soldProductIds.size}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                if (user.currentAddress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "位置: ${user.currentAddress}",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Message button
            OutlinedButton(
                onClick = onMessageClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RoseRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Chat, contentDescription = "Message", tint = RoseRed
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("发消息")
            }
//
//            // Follow button
//            Button(
//                onClick = onFollowClick,
//                modifier = Modifier.weight(1f),
//                shape = RoundedCornerShape(24.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = RoseRed
//                )
//            ) {
//                Text("关注")
//            }
        }
    }
}

@Composable
fun UserProductGrid(
    products: List<Product>, emptyMessage: String, onProductClick: (String) -> Unit
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage, color = Color.Gray, textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            items(products) { product ->
                ProductCard(
                    product = product.toAbstract(), onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
fun ErrorMessage(
    error: String, onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "加载失败", fontWeight = FontWeight.Bold, color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry, colors = ButtonDefaults.buttonColors(
                containerColor = RoseRed
            )
        ) {
            Text("重试")
        }
    }
}

@Composable
fun UserNotFound() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "未找到该用户",
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "该用户可能已注销或不存在", color = Color.Gray
        )
    }
}
