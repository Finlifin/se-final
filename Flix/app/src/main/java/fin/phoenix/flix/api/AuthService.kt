package fin.phoenix.flix.api

import com.google.gson.annotations.SerializedName
import fin.phoenix.flix.data.Account
import fin.phoenix.flix.data.User
import fin.phoenix.flix.data.UserAbstract
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login/password")
    suspend fun loginWithPassword(@Body request: LoginRequest.Password): Response<GenericApiResponse<AuthData>>

    @POST("auth/login/sms")
    suspend fun loginWithSms(@Body request: LoginRequest.Sms): Response<GenericApiResponse<AuthData>>

    @POST("auth/send_sms")
    suspend fun sendSmsCode(@Body request: SendSmsRequest): Response<GenericApiResponse<Unit>>
    
    @POST("auth/verify_token")
    suspend fun verifyToken(@Body request: VerifyTokenRequest): Response<GenericApiResponse<TokenVerificationData>>
    
    /**
     * 设置初始密码（适用于通过短信验证码注册但未设置密码的用户）
     */
    @POST("auth/set_password")
    suspend fun setInitialPassword(@Body request: SetPasswordRequest): Response<GenericApiResponse<Unit>>
    
    /**
     * 通过手机号+验证码设置密码
     */
    @POST("auth/set_password")
    suspend fun setInitialPasswordWithSms(@Body request: SetPasswordWithSmsRequest): Response<GenericApiResponse<Unit>>
    
    /**
     * 更新密码（修改已有密码）
     */
    @POST("auth/update_password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): Response<GenericApiResponse<Unit>>
    
    /**
     * 重置密码（忘记密码时使用）
     */
    @POST("auth/reset_password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericApiResponse<Unit>>
}

// 登录请求
sealed class LoginRequest {
    data class Password(
        @SerializedName("phone_number") val phoneNumber: String,
        @SerializedName("password") val password: String
    ) : LoginRequest()

    data class Sms(
        @SerializedName("phone_number") val phoneNumber: String,
        @SerializedName("sms_code") val smsCode: String
    ) : LoginRequest()
}

// 认证数据（登录响应中的data部分）
data class AuthData(
    @SerializedName("user") val user: UserAbstract,
    @SerializedName("token") val token: String,
    @SerializedName("account") val account: Account
)

// Token验证数据
data class TokenVerificationData(
    @SerializedName("claims") val claims: Map<String, Any>,
    @SerializedName("user") val user: UserAbstract
)

// 发送短信请求
data class SendSmsRequest(
    @SerializedName("phone_number") val phoneNumber: String
)

// Token验证请求
data class VerifyTokenRequest(
    @SerializedName("token") val token: String
)

// 设置初始密码请求
data class SetPasswordRequest(
    @SerializedName("new_password") val newPassword: String
)

// 通过手机号+验证码设置密码请求
data class SetPasswordWithSmsRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("sms_code") val smsCode: String,
    @SerializedName("new_password") val newPassword: String
)

// 更新密码请求
data class UpdatePasswordRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

// 重置密码请求
data class ResetPasswordRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("sms_code") val smsCode: String,
    @SerializedName("new_password") val newPassword: String
)
