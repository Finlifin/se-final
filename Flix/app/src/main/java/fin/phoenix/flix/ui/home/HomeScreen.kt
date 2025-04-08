package fin.phoenix.flix.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fin.phoenix.flix.data.ProductStatus
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.message.MessageCenterScreen
import fin.phoenix.flix.ui.myprofile.MyProfileContent
import kotlinx.coroutines.launch

// Define navigation destinations
sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    data object Browse : Screen("browse", Icons.Default.Search, "浏览")
    data object Favorites : Screen("favorites", Icons.Default.FavoriteBorder, "收藏")
    data object Inbox : Screen("messages", Icons.Default.Inbox, "消息")
    data object Profile : Screen("profile", Icons.Default.Person, "我的")
}

@Composable
fun HomeScreen(navController: NavController) {
    val mainNavController = rememberNavController()

    Scaffold(bottomBar = {
        NavigationBar {
            val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val screens = listOf(Screen.Browse, Screen.Favorites, Screen.Inbox, Screen.Profile)

            screens.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                    label = { Text(screen.label) },
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        mainNavController.navigate(screen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(mainNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    })
            }
        }
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = { navController.navigate("/publish") }, containerColor = RoseRed
        ) {
            Icon(
                imageVector = Icons.Default.Add, contentDescription = "Publish", tint = Color.White
            )
        }
    }) { paddingValues ->
        NavHost(
            navController = mainNavController,
            startDestination = Screen.Browse.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Browse.route) {
                BrowseScreen(navController)
            }

            // 你可能会问，主播主播，为什么不统一传递上下文呢

            composable(Screen.Favorites.route) {
                // Navigate to external favorites screen
                LaunchedEffectNavigation(navController, "/favorites")
            }

            composable(Screen.Inbox.route) {
                MessageCenterScreen(navController = navController)
            }

            composable(Screen.Profile.route) {
                // Get the current user ID from shared preferences
                val context = LocalContext.current
                val sharedPref = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
                val userId = sharedPref.getString("user_id", null) ?: ""

                MyProfileContent(navController = navController, userId = userId)
            }
        }
    }
}

@Composable
private fun LaunchedEffectNavigation(navController: NavController, route: String) {
    LaunchedEffect(key1 = true) {
        navController.navigate(route)
    }

    // Show a loading indicator while navigating
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = RoseRed)
    }
}

@Composable
private fun BrowseScreen(navController: NavController) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    BrowseContent(uiState, viewModel, navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseContent(
    uiState: HomeUiState, viewModel: HomeViewModel, navController: NavController
) {
    var isRefreshing: Boolean by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val onRefresh: () -> Unit = {
        viewModel.viewModelScope.launch {
            isRefreshing = true
            viewModel.refreshData()
            isRefreshing = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = state, isRefreshing = isRefreshing, onRefresh = onRefresh
            )
    ) {
        val categories =
            listOf("全部", "数码", "服装", "图书", "家具", "运动", "生活用品", "学习用品")

        HomeTopBar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onSearch = {
                viewModel.fetchProducts(
                    category = if (uiState.selectedCategory == "全部") null else uiState.selectedCategory,
                    searchQuery = uiState.searchQuery
                )
            },
            categories = categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.updateSelectedCategory(it) },
            onMessageClick = { navController.navigate("/messages") })

        Box {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RoseRed)
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "加载失败", color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.error, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "点击重试",
                            color = RoseRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.refreshData() })
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F8F8)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val availableProducts =
                        uiState.products.filter { it.status == ProductStatus.AVAILABLE }

                    if (availableProducts.isNotEmpty()) {
                        item {
                            RecentlyAddedSection(
                                products = availableProducts, onProductClick = { productId ->
                                    navController.navigate("/product/$productId")
                                })
                        }
                    }

                    if (uiState.sellers.isNotEmpty()) {
                        item {
                            PopularSellersSection(
                                sellers = uiState.sellers, onSellerClick = { sellerId ->
                                    navController.navigate("/profile/$sellerId")
                                })
                        }
                    }

                    item {
                        Text(
                            text = "所有商品",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (availableProducts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无商品", color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(availableProducts.chunked(2)) { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { product ->
                                    ProductCard(
                                        product = product,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("/product/${product.id}") })
                                }

                                // If we have an odd number of items, add an empty space
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

//            PullToRefreshContainer(
//                state = state,
//                modifier = Modifier.align(Alignment.TopCenter)
//            )
        }
    }
}

@Composable
private fun HomeTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onMessageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(text = "搜索商品、商家") },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = RoseRed,
                            modifier = Modifier.clickable { onSearch() })
                    }
                })

            Spacer(modifier = Modifier.width(8.dp))

            BadgedBox(
                badge = {
                    Badge(
                        containerColor = RoseRed, modifier = Modifier.size(8.dp)
                    )
                }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Messages",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onMessageClick() },
                    tint = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(categories) { category ->
                CategoryChip(
                    category = category,
                    selected = category == selectedCategory,
                    onSelected = { onCategorySelected(category) })
            }
        }
    }
}
