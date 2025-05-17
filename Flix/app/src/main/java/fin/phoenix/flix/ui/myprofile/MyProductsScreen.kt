package fin.phoenix.flix.ui.myprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.ProductStatus
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.home.ProductCard
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.ui.components.ErrorMessage
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductsScreen(navController: NavController, userId: String) {
    val viewModel: MyProfileViewModel = viewModel()
    
    // Observe products state
    val productsState by viewModel.userProductsState.observeAsState(initial = Resource.Loading)
    
    // Load products when composable is first composed
    LaunchedEffect(userId) {
        viewModel.getUserProducts(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "我发布的商品",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("/publish") },
                containerColor = RoseRed
            ) {
                Text("发布", color = Color.White)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
        ) {
            when (val state = productsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RoseRed
                    )
                }
                is Resource.Error -> {
                    ErrorMessage(
                        error = state.message ?: "加载失败",
                        onRetry = { viewModel.getUserProducts(userId) }
                    )
                }
                is Resource.Success -> {
                    val products = state.data
                    
                    if (products.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "您暂时没有发布商品",
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(products) { product ->
                                ProductCard(
                                    product = product.toAbstract(),
                                    onClick = { navController.navigate("/product/${product.id}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySoldProductsScreen(navController: NavController, userId: String) {
    val viewModel: MyProfileViewModel = viewModel()
    
    // Observe products state
    val soldProductsState by viewModel.userSoldProductsState.observeAsState(initial = Resource.Loading)
    
    // Load products when composable is first composed
    LaunchedEffect(userId) {
        viewModel.getUserSoldProducts(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我卖出的商品",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
        ) {
            when (val state = soldProductsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RoseRed
                    )
                }
                is Resource.Error -> {
                    ErrorMessage(
                        error = state.message ?: "加载失败",
                        onRetry = { viewModel.getUserSoldProducts(userId) }
                    )
                }
                is Resource.Success -> {
                    val products = state.data
                    
                    if (products.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "您暂时没有卖出商品",
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(products) { product ->
                                ProductCard(
                                    product = product.toAbstract(),
                                    onClick = { navController.navigate("/product/${product.id}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPurchasedProductsScreen(navController: NavController, userId: String) {
    val viewModel: MyProfileViewModel = viewModel()
    
    // Observe products state
    val purchasedProductsState by viewModel.userPurchasedProductsState.observeAsState(initial = Resource.Loading)
    
    // Load products when composable is first composed
    LaunchedEffect(userId) {
        viewModel.getUserPurchasedProducts(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "我买到的商品",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
        ) {
            when (val state = purchasedProductsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RoseRed
                    )
                }
                is Resource.Error -> {
                    ErrorMessage(
                        error = state.message ?: "加载失败",
                        onRetry = { viewModel.getUserPurchasedProducts(userId) }
                    )
                }
                is Resource.Success -> {
                    val products = state.data
                    
                    if (products.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "您暂时没有购买商品",
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(products) { product ->
                                ProductCard(
                                    product = product.toAbstract(),
                                    onClick = { navController.navigate("/product/${product.id}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
