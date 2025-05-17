package fin.phoenix.flix.api

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 从SharedPreferences获取token
        val sharedPref = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("auth_token", null)
        Log.d("AuthInterceptor", "Token: $savedToken")

        // 如果token存在，添加到请求头
        return if (savedToken != null) {
            val newRequest =
                originalRequest.newBuilder().header("Authorization", "Bearer $savedToken").build()
            chain.proceed(newRequest)
        } else {
            // 如果没有token，继续原始请求
            chain.proceed(originalRequest)
        }
    }
}