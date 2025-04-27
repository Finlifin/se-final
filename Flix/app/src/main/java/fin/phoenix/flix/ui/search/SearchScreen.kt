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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.ProductStatus
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.home.CategoryChip
import fin.phoenix.flix.ui.home.ProductCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    initialQuery: String = ""
) {
    val viewModel: SearchViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var tempQuery by remember { mutableStateOf(searchQuery) }
    var priceRangeExpanded by remember { mutableStateOf(false) }
    var sortByExpanded by remember { mutableStateOf(false) }
    var showPriceRangeDialog by remember { mutableStateOf(false) }
    var minPrice by remember { mutableDoubleStateOf(0.0) }
    var maxPrice by remember { mutableDoubleStateOf(10000.0) }
    
    val listState = rememberLazyListState()
    var isRefreshing = remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val onRefresh: () -> Unit = {
        isRefreshing.value = true
        scope.launch {
            isRefreshing.value = false
        }
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
    
    // 下拉刷新处理
    LaunchedEffect(isRefreshing) {
        if (isRefreshing.value) {
            delay(800) // 简单延迟，让刷新动画有足够时间显示
            if (searchQuery.isNotEmpty()) {
                viewModel.search(searchQuery)
            }
//            pullRefreshState.endRefresh()
        }
    }
    
    // 搜索延迟，避免频繁请求
    LaunchedEffect(tempQuery) {
        if (tempQuery != searchQuery) {
            delay(500) // 延迟500ms再执行搜索
            searchQuery = tempQuery
            if (searchQuery.isNotEmpty()) {
                viewModel.search(searchQuery)
            }
        }
    }
    
    // 初始搜索
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            searchQuery = initialQuery
            tempQuery = initialQuery
            viewModel.search(searchQuery)
        }
    }
    
    // 清理
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSearch()
        }
    }
    
    // 价格范围选择对话框
    if (showPriceRangeDialog) {
        Dialog(onDismissRequest = { showPriceRangeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp)
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
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (minPrice == 0.0) "" else minPrice.toString(),
                            onValueChange = { 
                                minPrice = it.toDoubleOrNull() ?: 0.0 
                            },
                            label = { Text("最低价格") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = if (maxPrice == 10000.0) "" else maxPrice.toString(),
                            onValueChange = { 
                                maxPrice = it.toDoubleOrNull() ?: 10000.0 
                            },
                            label = { Text("最高价格") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            modifier = Modifier
                                .clickable { 
                                    showPriceRangeDialog = false 
                                    viewModel.updatePriceRange(null, null)
                                }
                                .padding(8.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "清除",
                                color = Color.Gray
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    showPriceRangeDialog = false
                                    viewModel.updatePriceRange(minPrice, maxPrice)
                                }
                                .padding(8.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "确认",
                                color = RoseRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val searchFieldFocus = remember { FocusRequester() }
                    TextField(
                        value = tempQuery,
                        onValueChange = { tempQuery = it },
                        placeholder = { Text(text = "搜索商品") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFieldFocus),
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
                                    tempQuery = "" 
                                    searchQuery = ""
                                    viewModel.clearSearch()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "清除",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (tempQuery.isNotEmpty()) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.search(tempQuery)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = RoseRed
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = pullRefreshState,
                indicator = {
                    if (isRefreshing.value) {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing.value,
                        )
                    }
                },
                isRefreshing = isRefreshing.value,
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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 分类筛选
                            Box {
                                FilterButton(
                                    text = if (uiState.selectedCategory == null) "分类" else uiState.selectedCategory!!,
                                    onClick = { /* 由CategoryChips处理 */ }
                                )
                            }
                            
                            // 价格范围筛选
                            Box {
                                FilterButton(
                                    text = if (uiState.priceRange == null) "价格" 
                                    else "¥${uiState.priceRange!!.first}-${uiState.priceRange!!.second}",
                                    onClick = { showPriceRangeDialog = true }
                                )
                            }
                            
                            // 排序方式
                            Box {
                                FilterButton(
                                    text = when (uiState.sortBy) {
                                        "post_time" -> "最新"
                                        "price" -> if (uiState.sortOrder == "asc") "价格↑" else "价格↓"
                                        "view_count" -> "热门"
                                        else -> "综合"
                                    },
                                    onClick = { sortByExpanded = true }
                                )
                                
                                DropdownMenu(
                                    expanded = sortByExpanded,
                                    onDismissRequest = { sortByExpanded = false },
                                    properties = PopupProperties(focusable = true)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("最新") },
                                        onClick = {
                                            viewModel.updateSorting("post_time", "desc")
                                            sortByExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("价格从低到高") },
                                        onClick = {
                                            viewModel.updateSorting("price", "asc")
                                            sortByExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("价格从高到低") },
                                        onClick = {
                                            viewModel.updateSorting("price", "desc")
                                            sortByExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("热门") },
                                        onClick = {
                                            viewModel.updateSorting("view_count", "desc")
                                            sortByExpanded = false
                                        }
                                    )
                                }
                            }
                            
                            // 更多筛选
                            IconButton(onClick = { /* 显示更多筛选选项 */ }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "筛选",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    // 分类快速选择
                    if (uiState.categories.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                CategoryChip(
                                    category = "全部",
                                    selected = uiState.selectedCategory == null,
                                    onSelected = { viewModel.updateCategory(null) }
                                )
                            }
                            
                            items(uiState.categories) { category ->
                                CategoryChip(
                                    category = category,
                                    selected = uiState.selectedCategory == category,
                                    onSelected = { viewModel.updateCategory(category) }
                                )
                            }
                        }
                    }
                    
                    // 搜索结果
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.isLoading && uiState.products.isEmpty()) {
                            // 仅当没有数据且正在加载时显示全屏加载
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = RoseRed
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
                                val availableProducts = uiState.products.filter { it.status == ProductStatus.AVAILABLE }
                                items(availableProducts.chunked(2)) { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowItems.forEach { product ->
                                            ProductCard(
                                                product = product,
                                                modifier = Modifier.weight(1f),
                                                onClick = { navController.navigate("/product/${product.id}") }
                                            )
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
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp),
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