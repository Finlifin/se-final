package fin.phoenix.flix.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class UserManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
    
    private val _currentUserId = MutableLiveData<String?>(null)
    val currentUserId: LiveData<String?> = _currentUserId
    
    private val _currentUser = MutableLiveData<UserAbstract?>(null)
    val currentUser: LiveData<UserAbstract?> = _currentUser

    init {
        _currentUserId.value = prefs.getString(KEY_USER_ID, null)
        val userName = prefs.getString(KEY_USER_NAME, null)
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        val schoolId = prefs.getString(KEY_SCHOOL_ID, null)
        val campusId = prefs.getString(KEY_CAMPUS_ID, null)
        
        if (_currentUserId.value != null && userName != null) {
            _currentUser.value = UserAbstract(
                uid = _currentUserId.value!!,
                userName = userName,
                avatarUrl = avatarUrl,
                schoolId = schoolId,
                campusId = campusId
            )
        }
    }

    fun setCurrentUser(user: UserAbstract) {
        prefs.edit {
            putString(KEY_USER_ID, user.uid)
            putString(KEY_USER_NAME, user.userName)
            putString(KEY_AVATAR_URL, user.avatarUrl)
            putString(KEY_SCHOOL_ID, user.schoolId)
            putString(KEY_CAMPUS_ID, user.campusId)
        }
        _currentUserId.value = user.uid
        _currentUser.value = user
    }

    fun clearCurrentUser() {
        prefs.edit {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_AVATAR_URL)
            remove(KEY_SCHOOL_ID)
            remove(KEY_CAMPUS_ID)
        }
        _currentUserId.value = null
        _currentUser.value = null
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_SCHOOL_ID = "school_id"
        private const val KEY_CAMPUS_ID = "campus_id"

        @Volatile
        private var INSTANCE: UserManager? = null

        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}