package fin.phoenix.flix.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import fin.phoenix.flix.util.GsonConfig
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.70.141.134:4000/api/v1/"
//    private const val BASE_URL = "http://192.168.31.117:4000/api/v1/"
    private const val IMAGE_BASE_URL = "http://10.70.141.134:5000/api/v1/images/"
//    private const val IMAGE_BASE_URL = "http://192.168.31.117:5000/api/v1/images/"
    private const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MB
    private const val CACHE_MAX_AGE = 60 * 5 // 5 minutes
    private const val CACHE_MAX_STALE = 60 * 60 * 24 * 7 // 1 week

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

        // 如果提供了Context，添加认证拦截器和缓存
        if (context != null) {
            // 添加认证拦截器
            builder.addInterceptor(AuthInterceptor(context))
            
            // 设置缓存目录和大小
            val cacheDir = File(context.cacheDir, "http-cache")
            val cache = Cache(cacheDir, CACHE_SIZE.toLong())
            builder.cache(cache)
            
            // 添加离线缓存拦截器
            builder.addInterceptor(createOfflineCacheInterceptor(context))
            
            // 添加在线缓存拦截器
            builder.addNetworkInterceptor(createOnlineCacheInterceptor())
        }

        return builder.build()
    }
    
    // 检查网络是否连接
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
    
    // 创建在线缓存拦截器，添加缓存控制头
    private fun createOnlineCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val response = chain.proceed(chain.request())
            
            // 为响应设置缓存控制，定义缓存的有效期
            val cacheControl = CacheControl.Builder()
                .maxAge(CACHE_MAX_AGE, TimeUnit.SECONDS)
                .build()
                
            response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .build()
        }
    }
    
    // 创建离线缓存拦截器，在无网络时使用缓存
    private fun createOfflineCacheInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            
            // 如果没有网络连接，修改请求以使用缓存
            if (!isNetworkAvailable(context)) {
                val cacheControl = CacheControl.Builder()
                    .maxStale(CACHE_MAX_STALE, TimeUnit.SECONDS)
                    .onlyIfCached()
                    .build()
                    
                request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build()
            }
            
            chain.proceed(request)
        }
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
