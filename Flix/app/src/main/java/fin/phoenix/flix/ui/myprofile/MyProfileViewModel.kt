package fin.phoenix.flix.ui.myprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.api.ProfileUpdateRequest
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.School
import fin.phoenix.flix.data.Campus
import fin.phoenix.flix.data.User
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.repository.SchoolRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

class MyProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val schoolRepository = SchoolRepository(application)
    private val userManager = UserManager.getInstance(application)

    private val _userProfileState = MutableLiveData<Resource<User>>()
    val userProfileState: LiveData<Resource<User>> = _userProfileState

    private val _userProductsState = MutableLiveData<Resource<List<Product>>>()
    val userProductsState: LiveData<Resource<List<Product>>> = _userProductsState

    private val _userSoldProductsState = MutableLiveData<Resource<List<Product>>>()
    val userSoldProductsState: LiveData<Resource<List<Product>>> = _userSoldProductsState

    private val _userPurchasedProductsState = MutableLiveData<Resource<List<Product>>>()
    val userPurchasedProductsState: LiveData<Resource<List<Product>>> = _userPurchasedProductsState

    private val _userFavoritesState = MutableLiveData<Resource<List<Product>>>()
    val userFavoritesState: LiveData<Resource<List<Product>>> = _userFavoritesState
    
    // 添加学校和校区名称的状态
    private val _schoolNameState = MutableLiveData<Resource<String>>(Resource.Loading)
    val schoolNameState: LiveData<Resource<String>> = _schoolNameState
    
    private val _campusNameState = MutableLiveData<Resource<String>>(Resource.Loading)
    val campusNameState: LiveData<Resource<String>> = _campusNameState

    /**
     * 获取用户个人资料
     */
    fun getUserProfile(userId: String) {
        _userProfileState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserProfile(userId)
            _userProfileState.value = result
            
            // 如果获取成功，更新UserManager并获取学校和校区名称
            if (result is Resource.Success) {
                updateUserManager(result.data)
                
                // 获取学校和校区的名称
                result.data.schoolId?.let {
                    if (it.isNotEmpty()) {
                        getSchoolName(it)
                    } else {
                        _schoolNameState.value = Resource.Success("未设置")
                    }
                } ?: run {
                    _schoolNameState.value = Resource.Success("未设置")
                }
                
                result.data.campusId?.let {
                    if (it.isNotEmpty()) {
                        getCampusName(it)
                    } else {
                        _campusNameState.value = Resource.Success("未设置")
                    }
                } ?: run {
                    _campusNameState.value = Resource.Success("未设置")
                }
            }
        }
    }
    
    /**
     * 获取学校名称
     */
    private fun getSchoolName(schoolId: String) {
        _schoolNameState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val result = schoolRepository.getSchool(schoolId)
                when (result) {
                    is Resource.Success -> {
                        _schoolNameState.value = Resource.Success(result.data.name)
                    }
                    is Resource.Error -> {
                        _schoolNameState.value = Resource.Error(result.message ?: "获取学校信息失败")
                    }
                    is Resource.Loading -> {
                        _schoolNameState.value = Resource.Loading
                    }
                }
            } catch (e: Exception) {
                _schoolNameState.value = Resource.Error("获取学校信息失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取校区名称
     */
    private fun getCampusName(campusId: String) {
        _campusNameState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val result = schoolRepository.getCampus(campusId)
                when (result) {
                    is Resource.Success -> {
                        _campusNameState.value = Resource.Success(result.data.name)
                    }
                    is Resource.Error -> {
                        _campusNameState.value = Resource.Error(result.message ?: "获取校区信息失败")
                    }
                    is Resource.Loading -> {
                        _campusNameState.value = Resource.Loading
                    }
                }
            } catch (e: Exception) {
                _campusNameState.value = Resource.Error("获取校区信息失败: ${e.message}")
            }
        }
    }

    /**
     * 更新UserManager中的用户信息
     */
    private fun updateUserManager(user: User) {
        // 创建UserAbstract并更新到UserManager
        val userAbstract = UserAbstract(
            uid = user.uid,
            userName = user.userName,
            avatarUrl = user.avatarUrl,
            schoolId = user.schoolId,
            campusId = user.campusId
        )
        userManager.setCurrentUser(userAbstract)
    }

    /**
     * 获取用户发布的商品
     */
    fun getUserProducts(userId: String) {
        _userProductsState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserProducts(userId)
            _userProductsState.value = result
        }
    }

    /**
     * 获取用户已售商品
     */
    fun getUserSoldProducts(userId: String) {
        _userSoldProductsState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserSoldProducts(userId)
            _userSoldProductsState.value = result
        }
    }

    /**
     * 获取用户购买的商品
     */
    fun getUserPurchasedProducts(userId: String) {
        _userPurchasedProductsState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserPurchasedProducts(userId)
            _userPurchasedProductsState.value = result
        }
    }

    /**
     * 获取用户收藏的商品
     */
    fun getUserFavorites(userId: String) {
        _userFavoritesState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserFavorites(userId)
            _userFavoritesState.value = result
        }
    }

    /**
     * 更新用户个人资料
     */
    fun updateUserProfile(user: ProfileUpdateRequest, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateUserProfile(user)
            when (result) {
                is Resource.Success -> {
                    _userProfileState.value = result
                    onComplete(true)
                }
                is Resource.Error -> {
                    _userProfileState.value = result
                    onComplete(false)
                }
                else -> onComplete(false)
            }
        }
    }

    /**
     * 充值余额
     * 这个方法在用户界面上直接调用充值，但我们现在修改它来导航到充值界面
     */
    fun rechargeBalance(userId: String, amount: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.rechargeBalance(userId, amount)
            when (result) {
                is Resource.Success -> {
                    _userProfileState.value = result
                    onComplete(true)
                }
                is Resource.Error -> onComplete(false)
                else -> onComplete(false)
            }
        }
    }
    
    /**
     * 刷新用户信息
     * 这个方法用于在充值后刷新用户信息
     */
    fun refreshUserProfile(userId: String) {
        viewModelScope.launch {
            val result = repository.getUserProfile(userId)
            _userProfileState.value = result
        }
    }
}
