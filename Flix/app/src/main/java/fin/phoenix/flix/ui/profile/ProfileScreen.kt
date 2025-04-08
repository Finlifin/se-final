package fin.phoenix.flix.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(navController: NavController, userId: String) {
    val profileViewModel: ProfileViewModel = viewModel()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Observe states from ViewModel
    val userProfileState by profileViewModel.userProfileState.observeAsState(initial = Resource.Loading)
    val userProductsState by profileViewModel.userProductsState.observeAsState(initial = Resource.Loading)
    val userSoldProductsState by profileViewModel.userSoldProductsState.observeAsState(initial = Resource.Loading)

    // Load user data when composable is first composed
    LaunchedEffect(userId) {
        profileViewModel.getSellerProfile(userId)
        profileViewModel.getSellerProducts(userId)
        profileViewModel.getUserSoldProducts(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                when (val state = userProfileState) {
                    is Resource.Success -> {
                        Text(
                            text = state.data.userName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    else -> {
                        Text(
                            text = "用户资料", maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
            )
        }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
        ) {
            when (val profileState = userProfileState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), color = RoseRed
                    )
                }

                is Resource.Error -> {
                    ErrorMessage(
                        error = profileState.message,
                        onRetry = { profileViewModel.getSellerProfile(userId) })
                }

                is Resource.Success -> {
                    val user = profileState.data

                    Column {
                        // User profile header
                        UserProfileHeader(
                            user = user,
                            onMessageClick = { navController.navigate("/messages/$userId") },
                            onFollowClick = { /* Follow logic */ })

                        // Tabs
                        val tabs = listOf("在售商品", "已售商品")
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.White,
                            contentColor = RoseRed
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title) })
                            }
                        }

                        // Content based on selected tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                        ) {
                            when (selectedTabIndex) {
                                0 -> {
                                    // Available products
                                    when (val productsState = userProductsState) {
                                        is Resource.Loading -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center),
                                                color = RoseRed
                                            )
                                        }

                                        is Resource.Error -> {
                                            ErrorMessage(
                                                error = productsState.message,
                                                onRetry = { profileViewModel.getSellerProducts(userId) })
                                        }

                                        is Resource.Success -> {
                                            // Here we're still using mockProducts as a temporary solution
                                            // In a real app, you would fetch the actual products using the IDs
                                            val products = productsState.data

                                            UserProductGrid(
                                                products = products,
                                                emptyMessage = "该用户暂无在售商品",
                                                onProductClick = { productId ->
                                                    navController.navigate("/product/$productId")
                                                })
                                        }
                                    }
                                }

                                1 -> {
                                    // Sold products
                                    when (val soldProductsState = userSoldProductsState) {
                                        is Resource.Loading -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center),
                                                color = RoseRed
                                            )
                                        }

                                        is Resource.Error -> {
                                            ErrorMessage(
                                                error = soldProductsState.message, onRetry = {
                                                    profileViewModel.getUserSoldProducts(userId)
                                                })
                                        }

                                        is Resource.Success -> {
                                            // Here we're still using mockProducts as a temporary solution
                                            // In a real app, you would fetch the actual products using the IDs
                                            val products = soldProductsState.data

                                            UserProductGrid(
                                                products = products,
                                                emptyMessage = "该用户暂无已售商品",
                                                onProductClick = { productId ->
                                                    navController.navigate("/product/$productId")
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
