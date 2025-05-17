package fin.phoenix.flix.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.Seller
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)

    private val _userProfileState = MutableLiveData<Resource<Seller>>()
    val userProfileState: LiveData<Resource<Seller>> = _userProfileState

    private val _userProductsState = MutableLiveData<Resource<List<Product>>>()
    val userProductsState: LiveData<Resource<List<Product>>> = _userProductsState

    private val _userSoldProductsState = MutableLiveData<Resource<List<Product>>>()
    val userSoldProductsState: LiveData<Resource<List<Product>>> = _userSoldProductsState

    fun getSellerProfile(userId: String) {
        _userProfileState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getSellerProfile(userId)
            _userProfileState.value = result
        }
    }

    fun getSellerProducts(userId: String) {
        _userProductsState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserProducts(userId)
            _userProductsState.value = result
        }
    }

    fun getUserSoldProducts(userId: String) {
        _userSoldProductsState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.getUserSoldProducts(userId)
            _userSoldProductsState.value = result
        }
    }

}
