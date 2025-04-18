package fin.phoenix.flix.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId
    
    private val _currentUser = MutableStateFlow<UserAbstract?>(null)
    val currentUser: StateFlow<UserAbstract?> = _currentUser

    init {
        _currentUserId.value = prefs.getString(KEY_USER_ID, null)
        val userName = prefs.getString(KEY_USER_NAME, null)
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        if (_currentUserId.value != null && userName != null) {
            _currentUser.value = UserAbstract(
                uid = _currentUserId.value!!,
                userName = userName,
                avatarUrl = avatarUrl
            )
        }
    }

    fun setCurrentUser(user: UserAbstract) {
        prefs.edit {
            putString(KEY_USER_ID, user.uid)
            putString(KEY_USER_NAME, user.userName)
            putString(KEY_AVATAR_URL, user.avatarUrl)
        }
        _currentUserId.value = user.uid
        _currentUser.value = user
    }

    fun clearCurrentUser() {
        prefs.edit {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_AVATAR_URL)
        }
        _currentUserId.value = null
        _currentUser.value = null
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AVATAR_URL = "avatar_url"

        @Volatile
        private var INSTANCE: UserManager? = null

        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}