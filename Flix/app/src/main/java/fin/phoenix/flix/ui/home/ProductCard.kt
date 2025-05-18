package fin.phoenix.flix.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.data.ProductAbstract
import fin.phoenix.flix.data.ProductStatus
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl


@Composable
fun ProductCard(
    product: ProductAbstract,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl(product.image)),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )

                if (product.status == ProductStatus.SOLD) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "已售出",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (product.status == ProductStatus.DELETE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "卖家已删除",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¥${product.price}",
                        color = RoseRed,
                        fontWeight = FontWeight.Bold
                    )
//
//                    Icon(
//                        imageVector = Icons.Default.FavoriteBorder,
//                        contentDescription = "Favorite",
//                        tint = Color.Gray,
//                        modifier = Modifier.size(20.dp)
//                    )
                }
            }
        }
    }
}