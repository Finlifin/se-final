package fin.phoenix.flix.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.ProductAbstract
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SearchViewModel"
    private val productRepository = ProductRepository(application)
    private val profileRepository = ProfileRepository(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // 分类列表
    private val categories = listOf("数码", "服装", "图书", "家具", "运动", "生活用品", "学习用品")

    init {
        // 初始化分类列表
        _uiState.update { it.copy(categories = categories) }
    }

    /**
     * 执行搜索
     */
    fun search(
        query: String, page: Int = 1, limit: Int = 20
    ) {
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null, lastQuery = query) }

        viewModelScope.launch {
            try {
                val result = productRepository.getProducts(
                    page = page,
                    limit = limit,
                    category = _uiState.value.selectedCategory,
                    searchQuery = query,
                    priceRange = _uiState.value.priceRange,
                    sortBy = _uiState.value.sortBy,
                    sortOrder = _uiState.value.sortOrder
                )

                when (result) {
                    is Resource.Success -> {
                        val productListData = result.data
                        _uiState.update {
                            it.copy(
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
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false, error = result.message
                            )
                        }
                    }

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索出错", e)
                _uiState.update {
                    it.copy(
                        isLoading = false, error = e.message ?: "搜索失败"
                    )
                }
            }
        }
    }

    /**
     * 加载下一页
     */
    fun loadNextPage() {
        val currentQuery = _uiState.value.lastQuery
        if (currentQuery.isNotEmpty() && _uiState.value.currentPage < _uiState.value.totalPages) {
            search(currentQuery, _uiState.value.currentPage + 1)
        }
    }

    /**
     * 更新分类筛选
     */
    fun updateCategory(category: String?) {
        if (_uiState.value.selectedCategory == category) return

        _uiState.update { it.copy(selectedCategory = category) }
        // 如果当前有搜索词，则重新搜索
        if (_uiState.value.lastQuery.isNotEmpty()) {
            search(_uiState.value.lastQuery)
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

        if (_uiState.value.priceRange == priceRange) return

        _uiState.update { it.copy(priceRange = priceRange) }
        // 如果当前有搜索词，则重新搜索
        if (_uiState.value.lastQuery.isNotEmpty()) {
            search(_uiState.value.lastQuery)
        }
    }

    /**
     * 更新排序方式
     */
    fun updateSorting(sortBy: String, sortOrder: String = "desc") {
        if (_uiState.value.sortBy == sortBy && _uiState.value.sortOrder == sortOrder) return

        _uiState.update { it.copy(sortBy = sortBy, sortOrder = sortOrder) }
        // 如果当前有搜索词，则重新搜索
        if (_uiState.value.lastQuery.isNotEmpty()) {
            search(_uiState.value.lastQuery)
        }
    }

    /**
     * 清空搜索结果
     */
    fun clearSearch() {
        _uiState.update {
            SearchUiState(categories = categories)
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