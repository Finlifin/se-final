package fin.phoenix.flix.ui.myprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.User
import fin.phoenix.flix.repository.ImageRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

class ProfileEditViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = ProfileRepository(application)
    private val imageRepository = ImageRepository(application)

    private val _userState = MutableLiveData<Resource<User>>()
    val userState: LiveData<Resource<User>> = _userState

    private val _updateState = MutableLiveData<Resource<User>?>()
    val updateState: LiveData<Resource<User>?> = _updateState

    fun getUserProfile(userId: String) {
        _userState.value = Resource.Loading
        viewModelScope.launch {
            val result = profileRepository.getUserProfile(userId)
            _userState.value = result
        }
    }

    fun updateProfile(user: User, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = profileRepository.updateUserProfile(user)
            _updateState.value = result
            when (result) {
                is Resource.Success -> onComplete(true)
                else -> onComplete(false)
            }
        }
    }

    fun updateAvatar(userId: String, uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val uploadResult = imageRepository.uploadImage(uri)
            if (uploadResult.isSuccess) {
                val avatarUrl = uploadResult.getOrNull()
                if (avatarUrl != null) {
                    val result = profileRepository.updateAvatar(userId, avatarUrl)
                    when (result) {
                        is Resource.Success -> {
                            _userState.value = result
                            onComplete(true)
                        }
                        else -> onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } else {
                onComplete(false)
            }
        }
    }
}