package fin.phoenix.flix.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import fin.phoenix.flix.data.ProductAbstract
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.ui.colors.DarkRoseRed
import fin.phoenix.flix.ui.colors.LightRoseRed
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl


@Composable
fun CategoryChip(
    category: String, selected: Boolean, onSelected: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) LightRoseRed else Color(0xFFF5F5F5),
        modifier = Modifier.clickable { onSelected() }) {
        Text(
            text = category,
            color = if (selected) DarkRoseRed else Color.DarkGray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun RecentlyAddedSection(
    products: List<ProductAbstract>, onProductClick: (String) -> Unit
) {
    Column {
        Text(
            text = "最新上架",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(products.take(5)) { product ->
                FeaturedProductCard(
                    product = product, onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
fun FeaturedProductCard(
    product: ProductAbstract, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl(product.image)),
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Show condition tag if available
                product.condition?.let { condition ->
                    Surface(
                        color = Color(0x80000000),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = condition,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
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
                    } else {
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
}

@Composable
fun PopularSellersSection(
    sellers: List<UserAbstract>, onSellerClick: (String) -> Unit
) {
    Column {
        Text(
            text = "活跃卖家",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sellers.take(8)) { seller ->
                SellerItem(
                    seller = seller, onClick = { onSellerClick(seller.uid) })
            }
        }
    }
}

@Composable
fun SellerItem(
    seller: UserAbstract, onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }) {
        Image(
            painter = rememberAsyncImagePainter(model = seller.avatarUrl),
            contentDescription = seller.userName,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = seller.userName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp
        )
    }
}

