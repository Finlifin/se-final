package fin.phoenix.flix.ui.myprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.api.ProfileUpdateRequest
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.User
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

class MyProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)

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

    /**
     * 获取用户个人资料
     */
    fun getUserProfile(userId: String) {
        _userProfileState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserProfile(userId)
            _userProfileState.value = result
        }
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
}
