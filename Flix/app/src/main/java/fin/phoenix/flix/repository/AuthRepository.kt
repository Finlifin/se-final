package fin.phoenix.flix.repository

import android.content.Context
import fin.phoenix.flix.api.*
import fin.phoenix.flix.data.Account
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest

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
            val loginRequest = LoginRequest.Password(phoneNumber, hashPassword(password))
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

    /**
     * 设置初始密码（已登录状态）
     */
    suspend fun setInitialPassword(password: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val request = SetPasswordRequest(hashPassword(password))
            authService.setInitialPassword(request).toResource("设置密码失败")
        }

    /**
     * 通过手机号和短信验证码设置初始密码
     */
    suspend fun setInitialPasswordWithSms(phoneNumber: String, smsCode: String, password: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val request = SetPasswordWithSmsRequest(phoneNumber, smsCode, hashPassword(password))
            authService.setInitialPasswordWithSms(request).toResource("设置密码失败")
        }

    /**
     * 更新密码（修改已有密码）
     */
    suspend fun updatePassword(oldPassword: String, newPassword: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val request = UpdatePasswordRequest(hashPassword(oldPassword), hashPassword(newPassword))
            authService.updatePassword(request).toResource("修改密码失败")
        }

    /**
     * 重置密码（忘记密码时使用）
     */
    suspend fun resetPassword(phoneNumber: String, smsCode: String, newPassword: String): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val request = ResetPasswordRequest(phoneNumber, smsCode, hashPassword(newPassword))
            authService.resetPassword(request).toResource("重置密码失败")
        }

    /**
     * 检查当前用户是否已经设置密码
     */
    suspend fun checkPasswordSet(): Resource<Boolean> =
        withContext(Dispatchers.IO) {
            val response = authService.checkPasswordSet().toResource("获取密码状态失败")
            
            if (response is Resource.Success) {
                Resource.Success(response.data.hasPassword)
            } else if (response is Resource.Error) {
                Resource.Error(response.message)
            } else {
                Resource.Error("未知错误")
            }
        }

    /**
     * 验证密码是否符合规则：
     * 长度6-16字符，必须至少包含数字、大写字母、小写字母、特殊符号中的两种
     */
    fun validatePassword(password: String): Boolean {
        if (password.length < 6 || password.length > 16) {
            return false
        }

        var containsDigit = false
        var containsUpperCase = false
        var containsLowerCase = false
        var containsSpecial = false

        for (char in password) {
            when {
                char.isDigit() -> containsDigit = true
                char.isUpperCase() -> containsUpperCase = true
                char.isLowerCase() -> containsLowerCase = true
                !char.isLetterOrDigit() -> containsSpecial = true
            }
        }

        val categoryCount = listOf(containsDigit, containsUpperCase, containsLowerCase, containsSpecial)
            .count { it }

        return categoryCount >= 2
    }

    /**
     * 对密码进行SHA-256加密
     */
    fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return BigInteger(1, bytes).toString(16).padStart(64, '0')
    }
}
