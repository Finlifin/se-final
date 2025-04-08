package fin.phoenix.flix.repository

import android.content.Context
import fin.phoenix.flix.api.*
import fin.phoenix.flix.data.Account
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 认证仓库类，处理用户登录、验证等认证相关操作
 */
class AuthRepository(private val context: Context) {
    private val authService = RetrofitClient.createService(AuthService::class.java, context)

    /**
     * 使用密码登录
     */
    suspend fun loginWithPassword(phoneNumber: String, password: String): Resource<AuthData> =
        withContext(Dispatchers.IO) {
            val loginRequest = LoginRequest.Password(phoneNumber, password)
            authService.loginWithPassword(loginRequest).toResource("登录失败")
        }

    /**
     * 使用短信验证码登录
     */
    suspend fun loginWithSms(phoneNumber: String, smsCode: String): Resource<AuthData> =
        withContext(Dispatchers.IO) {
            val loginRequest = LoginRequest.Sms(phoneNumber, smsCode)
            authService.loginWithSms(loginRequest).toResource("登录失败")
        }

    /**
     * 发送短信验证码
     */
    suspend fun sendSmsCode(phoneNumber: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val request = SendSmsRequest(phoneNumber)
            authService.sendSmsCode(request).toResource("发送验证码失败")
        }

    /**
     * 验证令牌
     */
    suspend fun verifyToken(token: String): Resource<TokenVerificationData> =
        withContext(Dispatchers.IO) {
            val request = VerifyTokenRequest(token)
            authService.verifyToken(request).toResource("验证令牌失败")
        }

    /**
     * 获取用户信息（从令牌验证结果中提取）
     */
    suspend fun getUserFromToken(token: String): Resource<UserAbstract> =
        withContext(Dispatchers.IO) {
            val tokenVerification = verifyToken(token)
            if (tokenVerification is Resource.Success) {
                Resource.Success(tokenVerification.data.user)
            } else if (tokenVerification is Resource.Error) {
                Resource.Error(tokenVerification.message)
            } else {
                Resource.Error("获取用户信息失败")
            }
        }
}
