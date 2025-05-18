package fin.phoenix.flix.ui.myprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.api.ProfileUpdateRequest
import fin.phoenix.flix.api.toProfileUpdateRequestResource
import fin.phoenix.flix.data.School
import fin.phoenix.flix.data.Campus
import fin.phoenix.flix.data.User
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.repository.ImageRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.repository.SchoolRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileEditViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = ProfileRepository(application)
    private val imageRepository = ImageRepository(application)
    private val schoolRepository = SchoolRepository(application)
    private val userManager = UserManager.getInstance(application)

    private val _userState = MutableLiveData<Resource<ProfileUpdateRequest>>()
    val userState: LiveData<Resource<ProfileUpdateRequest>> = _userState

    private val _updateState = MutableLiveData<Resource<User>?>()
    val updateState: LiveData<Resource<User>?> = _updateState
    
    private val _avatarUpdateState = MutableLiveData<Resource<User>?>()
    val avatarUpdateState: LiveData<Resource<User>?> = _avatarUpdateState

    // 学校列表状态
    private val _schoolsState = MutableStateFlow<Resource<List<School>>>(Resource.Loading)
    val schoolsState: StateFlow<Resource<List<School>>> = _schoolsState.asStateFlow()

    // 校区列表状态
    private val _campusesState = MutableStateFlow<Resource<List<Campus>>>(Resource.Loading)
    val campusesState: StateFlow<Resource<List<Campus>>> = _campusesState.asStateFlow()

    // 初始化时加载学校列表
    init {
        loadSchools()
    }

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
                is Resource.Success -> {
                    // 更新UserManager
                    updateUserManager(result.data)
                    onComplete(true)
                }
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
                            // 更新UserManager
                            updateUserManager(result.data)
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
    
    // 更新UserManager
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

    // 加载所有学校
    fun loadSchools() {
        viewModelScope.launch {
            _schoolsState.value = Resource.Loading
            val result = schoolRepository.getSchools()
            _schoolsState.value = result
        }
    }

    // 根据学校ID加载校区
    fun loadCampuses(schoolId: String) {
        if (schoolId.isBlank() || schoolId == "未设置") {
            _campusesState.value = Resource.Success(emptyList())
            return
        }
        
        viewModelScope.launch {
            _campusesState.value = Resource.Loading
            val result = schoolRepository.getCampusesBySchoolId(schoolId)
            _campusesState.value = result
        }
    }
    
    // 添加新学校
    fun addSchool(name: String, code: String, onComplete: (success: Boolean, schoolId: String?, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = schoolRepository.createSchool(name)
                when (result) {
                    is Resource.Success -> {
                        // 重新加载学校列表
                        loadSchools()
                        onComplete(true, result.data.id, "学校添加成功")
                    }
                    is Resource.Error -> {
                        onComplete(false, null, result.message)
                    }
                    else -> onComplete(false, null, "添加失败")
                }
            } catch (e: Exception) {
                onComplete(false, null, e.message ?: "添加失败")
            }
        }
    }
    
    // 添加新校区
    fun addCampus(schoolId: String, name: String, address: String, onComplete: (success: Boolean, campusId: String?, message: String) -> Unit) {
        if (schoolId.isBlank() || schoolId == "未设置") {
            onComplete(false, null, "请先选择学校")
            return
        }
        
        viewModelScope.launch {
            try {
                val result = schoolRepository.createCampus(schoolId, name, address)
                when (result) {
                    is Resource.Success -> {
                        // 重新加载校区列表
                        loadCampuses(schoolId)
                        onComplete(true, result.data.id, "校区添加成功")
                    }
                    is Resource.Error -> {
                        onComplete(false, null, result.message ?: "添加失败")
                    }
                    else -> onComplete(false, null, "添加失败")
                }
            } catch (e: Exception) {
                onComplete(false, null, e.message ?: "添加失败")
            }
        }
    }
    
    // 根据关键词搜索学校
    fun searchSchools(query: String) {
        viewModelScope.launch {
            _schoolsState.value = Resource.Loading
            val result = schoolRepository.searchSchools(query)
            _schoolsState.value = result
        }
    }
}