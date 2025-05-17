package fin.phoenix.flix.ui.product

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.api.navigateToChat
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.colors.VeryLightRoseRed
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.imageUrl
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(navController: NavController, productId: String) {
    val viewModel: ProductDetailViewModel = viewModel()
    val productState by viewModel.productState.observeAsState(Resource.Loading)
    val sellerState by viewModel.sellerState.observeAsState(Resource.Loading)
    val isFavorited by viewModel.isProductFavorite.observeAsState(false)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val showPaymentOptions = remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadProductDetails(productId)
    }

    Scaffold(modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues()), topBar = {
        ProductDetailTopBar(
            onBackClick = { navController.popBackStack() },
            onShareClick = { 
                // 获取当前商品数据
                if (productState is Resource.Success) {
                    val product = (productState as Resource.Success<Product>).data
                    val shareText = "这是一件Flix上的商品: ${product.title}, https://example.com/product/${product.id}"
                    
                    // 创建分享意图
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    
                    // 创建选择器并启动分享活动
                    val shareIntent = Intent.createChooser(sendIntent, "分享商品")
                    context.startActivity(shareIntent)
                } else {
                    Toast.makeText(context, "商品信息加载中，请稍后再试", Toast.LENGTH_SHORT).show()
                }
            })
    }, bottomBar = {
        when (productState) {
            is Resource.Success -> {
                val product = (productState as Resource.Success<Product>).data
                BottomActionBar(
                    isFavorited = isFavorited,
                    isMyProduct = product.sellerId == viewModel.currentUserId,
                    onEditClick = { navController.navigate("/product/edit/${product.id}") },
                    onFavoriteClick = { viewModel.toggleFavorite() },
                    onContactClick = {
                        scope.launch {
                            navigateToChat(product.sellerId) {
                                 navController.navigate(it)
                            }
                        }
                    },
                    onBuyClick = { showPaymentOptions.value = true })
            }

            else -> {}
        }
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (productState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(color = RoseRed)
                }

                is Resource.Error -> {
                    Text(
                        text = "获取商品信息失败: ${(productState as Resource.Error).message}",
                        color = Color.Red
                    )
                }

                is Resource.Success -> {
                    val product = (productState as Resource.Success<Product>).data
                    ProductDetailContent(
                        product = product, sellerState = sellerState, navController = navController
                    )

                    if (showPaymentOptions.value) {
                        PaymentOptionsBottomSheet(
                            product = product,
                            onDismiss = { showPaymentOptions.value = false },
                            onProceedToPayment = { paymentMethod, deliveryMethod, address ->
                                viewModel.createOrder(
                                    productId = product.id,
                                    paymentMethod = paymentMethod,
                                    deliveryMethod = deliveryMethod,
                                    address = address,
                                    onSuccess = { orderId ->
                                        navController.navigate("/payment/confirm/$orderId")
                                        showPaymentOptions.value = false
                                    },
                                    onError = { errorMessage ->
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT)
                                            .show()
                                    })
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: Product,
    sellerState: Resource<UserAbstract>,
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 商品图片
        ProductImageCarousel(
            images = product.images, modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        // 价格和基本信息区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 价格信息带背景强调
            Surface(
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "¥", fontSize = 20.sp, color = RoseRed, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.price.toString(),
                        fontSize = 28.sp,
                        color = RoseRed,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${formatPostTime(product.postTime)}, ${product.viewCount}浏览",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = product.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 基本信息卡片
            Surface(
                shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, VeryLightRoseRed)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(label = "成色", value = product.condition)
                        InfoItem(label = "分类", value = product.category)
                        InfoItem(label = "收藏", value = "${product.favoriteCount}")
                    }
                }
            }

            // 显示标签（如果有）
            if (product.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(product.tags) { tag ->
                        ProductInfoChip(label = "#$tag")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 商品详情卡片
            Surface(
                shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, VeryLightRoseRed)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "商品详情", fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "发布于 ${formatPostTime(product.postTime)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description, lineHeight = 24.sp, color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 交易信息卡片
            Surface(
                shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, VeryLightRoseRed)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "交易信息", fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 交易地点
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "交易地点",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = product.location, color = Color.DarkGray, fontSize = 14.sp
                            )
                        }
                    }

                    // 配送方式 (如果有)
                    if (product.availableDeliveryMethods.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "配送方式",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(product.availableDeliveryMethods) { methodId ->
                                    val method = when (methodId) {
                                        "express" -> "快递配送"
                                        "pickup" -> "自提"
                                        "same_day" -> "当日达"
                                        "self_delivery" -> "卖家送货"
                                        "courier" -> "同城跑腿"
                                        else -> methodId
                                    }
                                    Text(
                                        text = method,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier
                                            .background(
                                                color = VeryLightRoseRed,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // 卖家信息卡片
            when (sellerState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(color = RoseRed)
                }

                is Resource.Error -> {
                    Text(
                        text = "获取卖家信息失败", color = Color.Red
                    )
                }

                is Resource.Success -> {
                    val seller = sellerState.data
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, VeryLightRoseRed)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painter = rememberAsyncImagePainter(model = seller.avatarUrl?.let {
                                imageUrl(
                                    it
                                )
                            } ?: "https://randomuser.me/api/portraits/lego/1.jpg"),
                                contentDescription = "User avatar",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = seller.userName, fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "信用分: 未实现", fontSize = 12.sp, color = Color.Gray
                                )
                            }

                            OutlinedButton(
                                onClick = { navController.navigate("/profile/${seller.uid}") },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("查看主页")
                            }
                        }
                    }
                }
            }

            // 底部空间，防止内容被底部操作栏遮挡
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun InfoItem(
    label: String, value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label, fontSize = 12.sp, color = Color.Gray
        )
        Text(
            text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProductDetailTopBar(
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share, contentDescription = "分享"
                )
            }
        }
    }
}

private fun formatPostTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val timestampMs = timestamp * 1000  // 将秒转为毫秒
    val diff = now - timestampMs

    Log.d("ProductDetailScreen", "formatPostTime: timestamp=$timestamp, now=$now, diff=$diff")

    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(timestampMs))  // 使用毫秒级时间戳
            date
        }
    }
}

@Composable
fun SellerInformation(seller: UserAbstract, onSellerClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = rememberAsyncImagePainter(model = seller.avatarUrl?.let { imageUrl(it) }
            ?: "https://randomuser.me/api/portraits/lego/1.jpg"),
            contentDescription = "User avatar",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = seller.userName, fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "信用分: 未实现", fontSize = 12.sp, color = Color.Gray
            )
        }

        OutlinedButton(
            onClick = onSellerClick, shape = RoundedCornerShape(16.dp)
        ) {
            Text("查看主页")
        }
    }
}
