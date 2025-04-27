package fin.phoenix.flix.ui.myprofile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyFavoritesUiState(
    val favorites: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MyFavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MyFavoritesUiState())
    val uiState: StateFlow<MyFavoritesUiState> = _uiState
    private val productRepository = ProductRepository(application)

    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                when (val result = productRepository.getFavoriteProducts(userId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                favorites = result.data.products,
                                isLoading = false,
                                error = null
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
                        // Loading state is already handled above
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false, error = "网络错误: ${e.localizedMessage}"
                    )
                }
                Log.e("MyFavoritesVM", "Exception loading favorites", e)
            }
        }
    }
}
