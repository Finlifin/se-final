package fin.phoenix.flix.ui.product

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.data.User
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl
import kotlinx.coroutines.delay

@Composable
fun ProductImageCarousel(
    images: List<String>, modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        Box(
            modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center
        ) {
            Text("无图片")
        }
        return
    }

    var currentPage by remember { mutableIntStateOf(0) }

    // 添加滑动状态
    val pagerState = rememberPagerState(
        initialPage = 0, pageCount = { images.size })

    // 同步滑动状态和当��页面
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }

    // 自动轮播效果
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // 每3秒更换一次图片
            if (images.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % images.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(modifier = modifier) {
        // 主图片区域使用HorizontalPager
        HorizontalPager(
            state = pagerState, modifier = Modifier.fillMaxSize()
        ) { page ->
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl(images[page])),
                contentDescription = "Product image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 图片指示点
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in images.indices) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (i == currentPage) RoseRed else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // 图片计数指示器
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${currentPage + 1}/${images.size}", color = Color.White, fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SellerInformation(
    seller: User, onSellerClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onSellerClick() }
        .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        // Seller avatar
        Image(
            painter = rememberAsyncImagePainter(model = seller.avatarUrl),
            contentDescription = "Seller avatar",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Seller info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = seller.userName, fontWeight = FontWeight.Medium, fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating stars (mock data - in real app this would come from seller's actual rating)
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.StarRate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (index < 4) RoseRed else Color.LightGray // Mock 4-star rating
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "已发布 ${seller.publishedProductIds.size} 件商品",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "View seller profile",
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ProductCard(
    product: fin.phoenix.flix.data.ProductAbstract,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        // Product image
        Image(
            painter = rememberAsyncImagePainter(model = imageUrl(product.image)),
            contentDescription = product.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop
        )

        // Product info
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = product.title,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "¥${product.price}", color = RoseRed, fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Seller info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.seller != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = product.seller.avatarUrl),
                        contentDescription = product.seller.userName,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                else {
                    Image(
                        painter = rememberAsyncImagePainter(model = ""),
                        contentDescription = "",
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = if(product.seller != null) product.seller.userName else "未知卖家",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
