package fin.phoenix.flix.ui.campus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.repository.SchoolRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 校内页面ViewModel
 * 管理校内商品数据和页面状态
 */
class CampusViewModel(application: Application) : AndroidViewModel(application) {
    private val schoolRepository = SchoolRepository(application)
    // 商品仓库
    private val productRepository = ProductRepository(application)
    
    // UI状态
    private val _uiState = MutableStateFlow(CampusUiState())
    val uiState: StateFlow<CampusUiState> = _uiState.asStateFlow()
    
    // 当前使用的学校和校区ID
    private var currentSchoolId: String = ""
    private var currentCampusId: String = ""
    
    /**
     * 加载校区商品
     */
    fun loadCampusProducts(schoolId: String, campusId: String) {
        // 保存当前使用的学校和校区ID
        currentSchoolId = schoolId
        currentCampusId = campusId
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            val schoolResult = schoolRepository.getSchool(schoolId)
            val campusResult = schoolRepository.getCampus(campusId)

            when(schoolResult) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            schoolName = schoolResult.data.name,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = schoolResult.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }

            when(campusResult) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            campusName = campusResult.data.name,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = campusResult.message
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
            
            // 通过ProductRepository加载校区商品
            val result = productRepository.getCampusProducts(schoolId, campusId)
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            products = result.data,
                            error = null
                        ) 
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            products = emptyList(),
                            error = result.message
                        ) 
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
    
    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    /**
     * 搜索校区内商品
     */
    fun searchCampusProducts() {
        // 检查搜索关键词是否为空
        if (_uiState.value.searchQuery.isBlank()) {
            // 如果为空，则重新加载所有商品
            loadCampusProducts(currentSchoolId, currentCampusId)
            return
        }
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            // 调用带有搜索参数的API
            val result = productRepository.getCampusProducts(
                schoolId = currentSchoolId,
                campusId = currentCampusId,
                searchQuery = _uiState.value.searchQuery
            )
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            products = result.data,
                            error = null
                        ) 
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        ) 
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}

/**
 * 校内页面UI状态
 */
data class CampusUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val schoolName: String? = null,
    val campusName: String? = null,
    val error: String? = null
)