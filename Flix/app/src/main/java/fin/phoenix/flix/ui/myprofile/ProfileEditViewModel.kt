package fin.phoenix.flix.ui.myprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.api.ProfileUpdateRequest
import fin.phoenix.flix.api.toProfileUpdateRequestResource
import fin.phoenix.flix.data.User
import fin.phoenix.flix.repository.ImageRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

class ProfileEditViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = ProfileRepository(application)
    private val imageRepository = ImageRepository(application)

    private val _userState = MutableLiveData<Resource<ProfileUpdateRequest>>()
    val userState: LiveData<Resource<ProfileUpdateRequest>> = _userState

    private val _updateState = MutableLiveData<Resource<User>?>()
    val updateState: LiveData<Resource<User>?> = _updateState
    
    private val _avatarUpdateState = MutableLiveData<Resource<User>?>()
    val avatarUpdateState: LiveData<Resource<User>?> = _avatarUpdateState

    fun getUserProfile(userId: String) {
        _userState.value = Resource.Loading
        viewModelScope.launch {
            val result = profileRepository.getUserProfile(userId)
            _userState.value = result.toProfileUpdateRequestResource()
        }
    }

    fun updateProfile(user: ProfileUpdateRequest, onComplete: (Boolean) -> Unit) {
        _updateState.value = Resource.Loading
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
        _avatarUpdateState.value = Resource.Loading
        viewModelScope.launch {
            val uploadResult = imageRepository.uploadImage(uri)
            if (uploadResult.isSuccess) {
                val avatarUrl = uploadResult.getOrNull()
                if (avatarUrl != null) {
                    val result = profileRepository.updateAvatar(userId, avatarUrl)
                    _avatarUpdateState.value = result
                    when (result) {
                        is Resource.Success -> {
                            _userState.value = result.toProfileUpdateRequestResource()
                            onComplete(true)
                        }
                        else -> onComplete(false)
                    }
                } else {
                    _avatarUpdateState.value = Resource.Error("头像上传失败")
                    onComplete(false)
                }
            } else {
                val errorMessage = uploadResult.exceptionOrNull()?.message ?: "未知错误"
                _avatarUpdateState.value = Resource.Error("头像上传失败: $errorMessage")
                onComplete(false)
            }
        }
    }
}