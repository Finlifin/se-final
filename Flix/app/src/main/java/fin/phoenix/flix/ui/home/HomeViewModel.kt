package fin.phoenix.flix.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.ProductAbstract
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
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
            fetchProducts()
            fetchPopularSellers()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing data.")
            e.printStackTrace()
        }
    }

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
                    val products = result.data.map { it.toProductAbstract() }
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
            // This would be better with a dedicated endpoint for popular sellers
            // For now, use a mock implementation or adapt from your API
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
        fetchProducts(category = if (category == "全部") null else category)
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
