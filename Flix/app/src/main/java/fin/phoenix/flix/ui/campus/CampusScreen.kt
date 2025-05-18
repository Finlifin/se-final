package fin.phoenix.flix.ui.campus

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.home.ProductCard

/**
 * 校内商品页面
 * 显示当前用户所在学校与校区的商品
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusScreen(navController: NavController) {
    // 获取ViewModel
    val viewModel: CampusViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // 获取当前登录用户
    val context = LocalContext.current
    val userManager = remember { UserManager.getInstance(context) }
    val currentUser by userManager.currentUser.observeAsState()

    // 当用户数据加载完成后，加载校区商品
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            if (!user.schoolId.isNullOrEmpty() && !user.campusId.isNullOrEmpty()) {
                viewModel.loadCampusProducts(user.schoolId, user.campusId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 用户未登录或未设置学校/校区
        if (currentUser == null || currentUser?.schoolId.isNullOrEmpty() || currentUser?.campusId.isNullOrEmpty()) {
            NotConfiguredContent(
                isLoggedIn = currentUser != null,
                onLoginClick = { navController.navigate("/login") },
                onSetupClick = { navController.navigate("/my_profile/edit") })
        } else {
            // 已登录且已设置学校/校区
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F8))
                    .padding(16.dp)
            ) {
                // 学校和校区信息
                SchoolInfoCard(
                    schoolName = uiState.schoolName ?: "未知学校",
                    campusName = uiState.campusName ?: "未知校区"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 搜索框
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { viewModel.searchCampusProducts() })

                Spacer(modifier = Modifier.height(16.dp))

                // 商品列表
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center), color = RoseRed
                            )
                        }

                        uiState.error != null -> {
                            ErrorContent(
                                error = uiState.error, onRetry = {
                                    if (currentUser != null && currentUser!!.schoolId != null && currentUser!!.campusId != null) {
                                        viewModel.loadCampusProducts(
                                            currentUser!!.schoolId!!,
                                            currentUser!!.campusId!!
                                        )
                                    }
                                })
                        }

                        uiState.products.isEmpty() -> {
                            EmptyContent()
                        }

                        else -> {
                            ProductList(
                                products = uiState.products, onProductClick = { productId ->
                                    navController.navigate("/product/$productId")
                                })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotConfiguredContent(
    isLoggedIn: Boolean, onLoginClick: () -> Unit, onSetupClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = RoseRed
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isLoggedIn) "您还未设置学校或校区" else "请先登录",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoggedIn) "设置您的学校和校区后，即可浏览校内商品"
            else "登录后设置您的学校和校区，即可浏览校内商品",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = if (isLoggedIn) onSetupClick else onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
        ) {
            Text(if (isLoggedIn) "去设置" else "去登录")
        }
    }
}

@Composable
private fun SchoolInfoCard(schoolName: String, campusName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = RoseRed,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = schoolName, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )

                Text(
                    text = campusName, color = Color.Gray, fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("搜索校内商品") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search, contentDescription = "搜索", tint = Color.Gray
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("搜索")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun ErrorContent(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "加载失败", fontWeight = FontWeight.Bold, fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error ?: "未知错误", color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
        ) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂无校内商品", fontWeight = FontWeight.Bold, fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "校内还没有人发布商品，快来发布第一个商品吧",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProductList(
    products: List<fin.phoenix.flix.data.Product>, onProductClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products.chunked(2)) { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { product ->
                    ProductCard(
                        product = product.toAbstract(),
                        modifier = Modifier.weight(1f),
                        onClick = { onProductClick(product.id) })
                }

                // 如果一行不足两个商品，添加空白填充
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}