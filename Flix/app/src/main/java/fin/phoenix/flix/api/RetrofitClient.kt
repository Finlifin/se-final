package fin.phoenix.flix.api

import android.content.Context
import fin.phoenix.flix.util.GsonConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://192.168.31.117:4000/api/v1/"
    private const val IMAGE_BASE_URL = "http://192.168.31.117:5000/api/v1/images/"

    // 缓存不同Context创建的Retrofit实例
    private val retrofitInstances = mutableMapOf<String, Retrofit>()

    private fun createOkHttpClient(context: Context? = null): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // 如果提供了Context，添加认证拦截器
        if (context != null) {
            builder.addInterceptor(AuthInterceptor(context))
        }

        return builder.build()
    }

    private fun getRetrofit(baseUrl: String, context: Context? = null): Retrofit {
        // 为每个Context+URL组合创建唯一的key
        val key = "${context?.hashCode() ?: "noContext"}_$baseUrl"

        // 如果缓存中没有，则创建新的Retrofit实例
        if (!retrofitInstances.containsKey(key)) {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(createOkHttpClient(context))
                .addConverterFactory(GsonConverterFactory.create(GsonConfig.createPrettyGson()))
                .build()

            retrofitInstances[key] = retrofit
        }

        return retrofitInstances[key]!!
    }

    fun <T> createService(
        serviceClass: Class<T>,
        context: Context? = null,
        useImageBaseUrl: Boolean = false
    ): T {
        val baseUrl = if (useImageBaseUrl) IMAGE_BASE_URL else BASE_URL
        return getRetrofit(baseUrl, context).create(serviceClass)
    }
}