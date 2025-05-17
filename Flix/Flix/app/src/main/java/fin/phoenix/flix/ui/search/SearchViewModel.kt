package fin.phoenix.flix.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.ProductAbstract
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SearchViewModel"
    private val productRepository = ProductRepository(application)
    private val profileRepository = ProfileRepository(application)

    private val _uiState = MutableLiveData(SearchUiState())
    val uiState: LiveData<SearchUiState> = _uiState
    
    // 搜索相关状态
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery
    
    private val _tempQuery = MutableLiveData<String>("")
    val tempQuery: LiveData<String> = _tempQuery
    
    // 筛选条件状态
    private val _showPriceRangeDialog = MutableLiveData<Boolean>(false)
    val showPriceRangeDialog: LiveData<Boolean> = _showPriceRangeDialog
    
    private val _showSortDialog = MutableLiveData<Boolean>(false)
    val showSortDialog: LiveData<Boolean> = _showSortDialog
    
    private val _minPrice = MutableLiveData<Double>(0.0)
    val minPrice: LiveData<Double> = _minPrice
    
    private val _maxPrice = MutableLiveData<Double>(10000.0)
    val maxPrice: LiveData<Double> = _maxPrice
    
    private val _tempSortBy = MutableLiveData<String>("")
    val tempSortBy: LiveData<String> = _tempSortBy
    
    private val _tempSortOrder = MutableLiveData<String>("")
    val tempSortOrder: LiveData<String> = _tempSortOrder
    
    // 刷新状态
    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // 分类列表
    private val categories = listOf("数码", "服装", "图书", "家具", "运动", "生活用品", "学习用品")

    init {
        // 初始化分类列表
        _uiState.value = _uiState.value?.copy(categories = categories)
    }
    
    // UI 事件处理函数
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            search(query)
        }
    }
    
    fun setTempQuery(query: String) {
        _tempQuery.value = query
    }
    
    fun confirmSearch() {
        _tempQuery.value?.let { 
            if (it.isNotEmpty()) {
                _searchQuery.value = it
                search(it)
            }
        }
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _tempQuery.value = ""
        _uiState.value = SearchUiState(categories = categories)
    }
    
    fun showPriceRangeDialog(show: Boolean) {
        _showPriceRangeDialog.value = show
    }
    
    fun showSortDialog(show: Boolean) {
        _showSortDialog.value = show
    }
    
    fun setMinPrice(price: Double) {
        _minPrice.value = price
    }
    
    fun setMaxPrice(price: Double) {
        _maxPrice.value = price
    }
    
    fun setTempSortParams(sortBy: String, sortOrder: String) {
        _tempSortBy.value = sortBy
        _tempSortOrder.value = sortOrder
    }
    
    fun confirmPriceRange() {
        val min = _minPrice.value ?: 0.0
        val max = _maxPrice.value ?: 10000.0
        updatePriceRange(min, max)
        _showPriceRangeDialog.value = false
    }
    
    fun clearPriceRange() {
        updatePriceRange(null, null)
        _showPriceRangeDialog.value = false
    }
    
    fun confirmSorting() {
        val sortBy = _tempSortBy.value ?: "post_time"
        val sortOrder = _tempSortOrder.value ?: "desc"
        if (sortBy.isNotEmpty() && sortOrder.isNotEmpty()) {
            updateSorting(sortBy, sortOrder)
        }
        _showSortDialog.value = false
    }
    
    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
        if (refreshing) {
            _searchQuery.value?.let {
                if (it.isNotEmpty()) {
                    search(it)
                }
            }
            _isRefreshing.value = false
        }
    }

    /**
     * 执行搜索
     */
    fun search(
        query: String, page: Int = 1, limit: Int = 20
    ) {
        if (query.isBlank()) return

        _uiState.value = _uiState.value?.copy(isLoading = true, error = null, lastQuery = query)

        viewModelScope.launch {
            try {
                val result = productRepository.getProducts(
                    page = page,
                    limit = limit,
                    category = _uiState.value?.selectedCategory,
                    searchQuery = query,
                    priceRange = _uiState.value?.priceRange,
                    sortBy = _uiState.value?.sortBy,
                    sortOrder = _uiState.value?.sortOrder
                )

                when (result) {
                    is Resource.Success -> {
                        val productListData = result.data
                        _uiState.value = _uiState.value?.copy(
                            isLoading = false,
                            error = null,
                            products = productListData.products.map { product ->
                                when (val response =
                                    profileRepository.getUserAbstract(product.sellerId)) {
                                    is Resource.Success -> {
                                        ProductAbstract(
                                            id = product.id,
                                            title = product.title,
                                            price = product.price,
                                            image = product.images.firstOrNull() ?: "",
                                            condition = product.condition,
                                            seller = response.data,
                                            status = product.status
                                        )
                                    }

                                    is Resource.Error -> {
                                        Log.d(TAG, "获取卖家信息失败${response.message}")
                                        ProductAbstract(
                                            id = product.id,
                                            title = product.title,
                                            price = product.price,
                                            image = product.images.firstOrNull() ?: "",
                                            condition = product.condition,
                                            seller = null,
                                            status = product.status
                                        )
                                    }

                                    is Resource.Loading -> {
                                        ProductAbstract(
                                            id = product.id,
                                            title = product.title,
                                            price = product.price,
                                            image = product.images.firstOrNull() ?: "",
                                            condition = product.condition,
                                            seller = null,
                                            status = product.status
                                        )
                                    }
                                }
                            },
                            currentPage = productListData.currentPage,
                            totalPages = productListData.totalPages,
                            totalCount = productListData.totalCount
                        )
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value?.copy(
                            isLoading = false, error = result.message
                        )
                    }

                    is Resource.Loading -> {
                        _uiState.value = _uiState.value?.copy(isLoading = true)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索出错", e)
                _uiState.value = _uiState.value?.copy(
                    isLoading = false, error = e.message ?: "搜索失败"
                )
            }
        }
    }

    /**
     * 加载下一页
     */
    fun loadNextPage() {
        val currentQuery = _uiState.value?.lastQuery ?: ""
        val currentPage = _uiState.value?.currentPage ?: 1
        val totalPages = _uiState.value?.totalPages ?: 1
        if (currentQuery.isNotEmpty() && currentPage < totalPages) {
            search(currentQuery, currentPage + 1)
        }
    }

    /**
     * 更新分类筛选
     */
    fun updateCategory(category: String?) {
        if (_uiState.value?.selectedCategory == category) return

        _uiState.value = _uiState.value?.copy(selectedCategory = category)
        // 如果当前有搜索词，则重新搜索
        val currentQuery = _uiState.value?.lastQuery ?: ""
        if (currentQuery.isNotEmpty()) {
            search(currentQuery)
        }
    }

    /**
     * 更新价格范围筛选
     */
    fun updatePriceRange(minPrice: Double?, maxPrice: Double?) {
        val priceRange = if (minPrice != null && maxPrice != null) {
            Pair(minPrice, maxPrice)
        } else {
            null
        }

        if (_uiState.value?.priceRange == priceRange) return

        _uiState.value = _uiState.value?.copy(priceRange = priceRange)
        // 如果当前有搜索词，则重新搜索
        val currentQuery = _uiState.value?.lastQuery ?: ""
        if (currentQuery.isNotEmpty()) {
            search(currentQuery)
        }
    }

    /**
     * 更新排序方式
     */
    fun updateSorting(sortBy: String, sortOrder: String = "desc") {
        if (_uiState.value?.sortBy == sortBy && _uiState.value?.sortOrder == sortOrder) return

        _uiState.value = _uiState.value?.copy(sortBy = sortBy, sortOrder = sortOrder)
        // 如果当前有搜索词，则重新搜索
        val currentQuery = _uiState.value?.lastQuery ?: ""
        if (currentQuery.isNotEmpty()) {
            search(currentQuery)
        }
    }
}

/**
 * 搜索页面UI状态
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val products: List<ProductAbstract> = emptyList(),
    val lastQuery: String = "",
    val selectedCategory: String? = null,
    val priceRange: Pair<Double, Double>? = null,
    val sortBy: String = "post_time", // 默认按发布时间排序
    val sortOrder: String = "desc", // 默认降序
    val categories: List<String> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0
)