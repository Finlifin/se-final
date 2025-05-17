package fin.phoenix.flix.ui.myprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fin.phoenix.flix.data.User
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RechargeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)

    private val _rechargeState = MutableLiveData<RechargeState>()
    val rechargeState: LiveData<RechargeState> = _rechargeState

    /**
     * 充值余额
     */
    fun rechargeBalance(userId: String, amount: Int) {
        _rechargeState.value = RechargeState.Loading
        viewModelScope.launch {
            try {
                // 在实际情况中，这里应该接入第三方支付API
                // 为了演示，我们直接调用Repository的充值方法
                // 模拟网络延迟
                delay(1500)
                
                val result = repository.rechargeBalance(userId, amount)
                when (result) {
                    is Resource.Success -> {
                        _rechargeState.value = RechargeState.Success(result.data)
                    }
                    is Resource.Error -> {
                        _rechargeState.value = RechargeState.Error(result.message)
                    }
                    else -> {
                        _rechargeState.value = RechargeState.Error("充值失败，请稍后重试")
                    }
                }
            } catch (e: Exception) {
                _rechargeState.value = RechargeState.Error(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 充值状态
     */
    sealed class RechargeState {
        data object Loading : RechargeState()
        data class Success(val user: User) : RechargeState()
        data class Error(val message: String) : RechargeState()
    }
}
