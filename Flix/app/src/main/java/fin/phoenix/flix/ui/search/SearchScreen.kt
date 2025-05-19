package fin.phoenix.flix.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.ProductStatus
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.home.CategoryChip
import fin.phoenix.flix.ui.home.ProductCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    navController: NavController, initialQuery: String = ""
) {
    // 使用主Activity作为ViewModel的存储位置，确保整个应用中共享同一个ViewModel实例
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val viewModel: SearchViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner
    )
    val uiState by viewModel.uiState.observeAsState(SearchUiState())
    val searchQuery by viewModel.searchQuery.observeAsState("")
    val tempQuery by viewModel.tempQuery.observeAsState("")
    val showPriceRangeDialog by viewModel.showPriceRangeDialog.observeAsState(false)
    val showSortDialog by viewModel.showSortDialog.observeAsState(false)
    val minPrice by viewModel.minPrice.observeAsState(0.0)
    val maxPrice by viewModel.maxPrice.observeAsState(100000000.0) // 修改默认最大值为一亿
    val tempSortBy by viewModel.tempSortBy.observeAsState("")
    val tempSortOrder by viewModel.tempSortOrder.observeAsState("")
    val isRefreshing by viewModel.isRefreshing.observeAsState(false)

    rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    val onRefresh: () -> Unit = {
        viewModel.setRefreshing(true)
    }

    // 监测是否到达列表底部，用于自动加载下一页
    val reachedBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    // 自动加载下一页
    LaunchedEffect(reachedBottom) {
        if (reachedBottom && !uiState.isLoading && uiState.currentPage < uiState.totalPages) {
            viewModel.loadNextPage()
        }
    }

    // 搜索延迟，避免频繁请求
    LaunchedEffect(tempQuery) {
        if (tempQuery != searchQuery) {
            delay(500) // 延迟500ms再执行搜索
            viewModel.setSearchQuery(tempQuery)
        }
    }

    // 初始搜索
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            viewModel.setTempQuery(initialQuery)
            viewModel.setSearchQuery(initialQuery)
        }
    }

//    // 清理
//    DisposableEffect(Unit) {
//        onDispose {
//            viewModel.clearSearch()
//        }
//    }

    // 价格范围选择对话框
    if (showPriceRangeDialog) {
        Dialog(onDismissRequest = { viewModel.showPriceRangeDialog(false) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "设置价格范围",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (minPrice == 0.0) "" else minPrice.toString(),
                            onValueChange = {
                                viewModel.setMinPrice(it.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text("最低价格") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = if (maxPrice == 100000000.0) "" else maxPrice.toString(), // 修改为一亿
                            onValueChange = {
                                viewModel.setMaxPrice(it.toDoubleOrNull() ?: 100000000.0) // 修改为一亿
                            }, label = { Text("最高价格") }, singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                    ) {
                        Surface(modifier = Modifier
                            .clickable {
                                viewModel.clearPriceRange()
                            }
                            .padding(8.dp), color = Color.Transparent) {
                            Text(
                                text = "清除", color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Surface(modifier = Modifier
                            .clickable {
                                viewModel.confirmPriceRange()
                            }
                            .padding(8.dp), color = Color.Transparent) {
                            Text(
                                text = "确认", color = RoseRed, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // 排序选择对话框
    if (showSortDialog) {
        Dialog(onDismissRequest = { viewModel.showSortDialog(false) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "选择排序方式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTempSortParams("post_time", "desc")
                                },
                            color = if (tempSortBy == "post_time" && tempSortOrder == "desc") Color(
                                0xFFEAEAEA
                            ) else Color.Transparent
                        ) {
                            Text(
                                text = "最新",
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTempSortParams("price", "asc")
                                },
                            color = if (tempSortBy == "price" && tempSortOrder == "asc") Color(
                                0xFFEAEAEA
                            ) else Color.Transparent
                        ) {
                            Text(
                                text = "价格从低到高",
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTempSortParams("price", "desc")
                                },
                            color = if (tempSortBy == "price" && tempSortOrder == "desc") Color(
                                0xFFEAEAEA
                            ) else Color.Transparent
                        ) {
                            Text(
                                text = "价格从高到低",
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTempSortParams("view_count", "desc")
                                },
                            color = if (tempSortBy == "view_count" && tempSortOrder == "desc") Color(
                                0xFFEAEAEA
                            ) else Color.Transparent
                        ) {
                            Text(
                                text = "热门",
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                    ) {
                        Surface(modifier = Modifier
                            .clickable {
                                viewModel.showSortDialog(false)
                            }
                            .padding(8.dp), color = Color.Transparent) {
                            Text(
                                text = "取消", color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Surface(modifier = Modifier
                            .clickable {
                                viewModel.confirmSorting()
                            }
                            .padding(8.dp), color = Color.Transparent) {
                            Text(
                                text = "确认", color = RoseRed, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                val searchFieldFocus = remember { FocusRequester() }
                TextField(
                    value = tempQuery,
                    onValueChange = { viewModel.setTempQuery(it) },
                    placeholder = { Text(text = "搜索商品") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFieldFocus)
                        .padding(vertical = 2.dp), // 更紧凑
                    singleLine = true,
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
                            contentDescription = "搜索",
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        if (tempQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.clearSearch()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = Color.Gray
                                )
                            }
                        }
                    })
            }, navigationIcon = {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(36.dp) // 更小
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }, actions = {
                IconButton(
                    onClick = {
                        if (tempQuery.isNotEmpty()) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.confirmSearch()
                        }
                    },
                    modifier = Modifier.size(36.dp) // 更小
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = RoseRed
                    )
                }
            })
        }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = pullRefreshState,
                indicator = {
                    if (isRefreshing) {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                        )
                    }
                },
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 筛选栏
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp), // 更紧凑
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 价格范围筛选
                            Box {
                                FilterButton(
                                    text = if (uiState.priceRange == null) "价格"
                                    else "¥${uiState.priceRange!!.first}-${uiState.priceRange!!.second}",
                                    onClick = { viewModel.showPriceRangeDialog(true) })
                            }

                            // 排序方式
                            Box {
                                FilterButton(
                                    text = when (uiState.sortBy) {
                                        "post_time" -> "最新"
                                        "price" -> if (uiState.sortOrder == "asc") "价格↑" else "价格↓"
                                        "view_count" -> "热门"
                                        else -> "综合"
                                    }, onClick = { viewModel.showSortDialog(true) })
                            }
//
//                            // 更多筛选
//                            IconButton(
//                                onClick = { /* 显示更多筛选选项 */ },
//                                modifier = Modifier.size(28.dp) // 更小
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.FilterList,
//                                    contentDescription = "筛选",
//                                    tint = Color.DarkGray,
//                                    modifier = Modifier.size(18.dp) // 更小
//                                )
//                            }
                        }
                    }

                    // 分类快速选择
                    if (uiState.categories.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp), // 更紧凑
                            horizontalArrangement = Arrangement.spacedBy(4.dp) // 更紧凑
                        ) {
                            item {
                                CategoryChip(
                                    category = "全部",
                                    selected = uiState.selectedCategory == null,
                                    onSelected = { viewModel.updateCategory(null) })
                            }

                            items(uiState.categories) { category ->
                                CategoryChip(
                                    category = category,
                                    selected = uiState.selectedCategory == category,
                                    onSelected = { viewModel.updateCategory(category) })
                            }
                        }
                    }

                    // 搜索结果
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.isLoading && uiState.products.isEmpty()) {
                            // 仅当没有数据且正在加载时显示全屏加载
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center), color = RoseRed
                            )
                        } else if (uiState.error != null && uiState.products.isEmpty()) {
                            // 搜索错误且没有数据时显示错误信息
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "搜索失败",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.error.toString(),
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (uiState.products.isEmpty() && searchQuery.isNotEmpty()) {
                            // 搜索结果为空时的提示
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "没有找到相关商品",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "尝试使用不同的搜索词或筛选条件",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (searchQuery.isEmpty() && uiState.products.isEmpty()) {
                            // 初始状态，未执行搜索
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "搜索你想要的商品",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "输入关键词开始搜索",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // 搜索结果展示
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // 搜索结果计数
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "找到 ${uiState.totalCount} 件商品",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "第 ${uiState.currentPage}/${uiState.totalPages} 页",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                // 商品网格
                                val availableProducts =
                                    uiState.products.filter { it.status == ProductStatus.AVAILABLE }
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

                                        // 如果一行中的商品数量不足2个，添加一个空白区域保持对齐
                                        if (rowItems.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                // 显示加载更多指示器
                                if (uiState.isLoading && uiState.products.isNotEmpty() && uiState.currentPage < uiState.totalPages) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = RoseRed,
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }

                                // 底部留白
                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    text: String, onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.DarkGray
            )
        }
    }
}