package fin.phoenix.flix.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.ProductAbstract
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "HomeViewModel"
    private val productRepository = ProductRepository(application)
    private val userRepository = ProfileRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // 产品分页数据流
    private var _productsFlow: Flow<PagingData<ProductAbstract>>? = null
    val productsFlow: Flow<PagingData<ProductAbstract>>
        get() {
            val newFlow = _productsFlow ?: getProductsStream(
                category = _uiState.value.selectedCategory,
                searchQuery = _uiState.value.searchQuery
            ).cachedIn(viewModelScope)
            _productsFlow = newFlow
            return newFlow
        }

    init {
        try {
            refreshData()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing HomeViewModel.")
            e.printStackTrace()
        }
    }

    fun refreshData() {
        try {
            // 更新Paging数据流
            _productsFlow = getProductsStream(
                category = _uiState.value.selectedCategory,
                searchQuery = _uiState.value.searchQuery
            ).cachedIn(viewModelScope)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing data.")
            e.printStackTrace()
        }
    }
    
    // 创建产品分页数据流
    private fun getProductsStream(
        category: String? = null,
        searchQuery: String? = null
    ): Flow<PagingData<ProductAbstract>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
                initialLoadSize = 10
            ),
            pagingSourceFactory = {
                ProductPagingSource(
                    repository = productRepository,
                    category = if (category == "全部") null else category,
                    searchQuery = searchQuery,
                    sortBy = "postTime",
                    sortOrder = "desc"
                )
            }
        ).flow
    }

    // 此方法保留用于加载一次性数据，例如热门商品和热门卖家
    fun fetchProducts(
        category: String? = null, searchQuery: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = productRepository.getProducts(
                category = if (category == "全部") null else category,
                searchQuery = searchQuery,
                sortBy = "postTime",
                sortOrder = "desc"
            )

            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "result: ${result.data}")
                    val products = result.data.products.map { it.toProductAbstract() }
                    _uiState.update {
                        it.copy(
                            isLoading = false, products = products, error = null
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
        }
    }

    private fun fetchPopularSellers() {
        viewModelScope.launch {
            // 获取热门卖家
            when (val result = userRepository.getPopularSellers()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(sellers = result.data) }
                }

                is Resource.Error -> {
                    // Silently fail for sellers section
                }

                is Resource.Loading -> {
                    // Silently fail for sellers section
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        // 使用新分类更新分页数据流
        _productsFlow = getProductsStream(
            category = category,
            searchQuery = _uiState.value.searchQuery
        ).cachedIn(viewModelScope)
    }

    // Helper extension function to convert Product to ProductAbstract
    private fun Product.toProductAbstract(): ProductAbstract {
        return ProductAbstract(
            id = this.id,
            title = this.title,
            price = this.price,
            image = this.images.firstOrNull() ?: "",
            condition = this.condition,
            seller = UserAbstract(
                uid = this.sellerId,
                userName = "",  // Need to fetch from user repository in a real app
                avatarUrl = ""  // Need to fetch from user repository in a real app
            ),
            status = this.status
        )
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val products: List<ProductAbstract> = emptyList(),
    val sellers: List<UserAbstract> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "全部",
    val error: String? = null
)
