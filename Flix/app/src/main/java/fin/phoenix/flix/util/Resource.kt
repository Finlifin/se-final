package fin.phoenix.flix.util

import com.google.gson.Gson
import fin.phoenix.flix.api.ErrorResponse
import fin.phoenix.flix.api.GenericApiResponse
import retrofit2.Response
import java.io.IOException

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

/**
 * 统一处理API响应的扩展函数
 */
fun <T> Response<GenericApiResponse<T>>.toResource(defaultErrorMessage: String): Resource<T> {
    return try {
        if (isSuccessful) {
            val body = body()
            if (body != null) {
                if (body.success && body.data != null) {
                    Resource.Success(body.data)
                } else {
                    Resource.Error(body.message ?: defaultErrorMessage)
                }
            } else {
                Resource.Error(defaultErrorMessage)
            }
        } else {
            val errorBody = errorBody()?.string()
            try {
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                Resource.Error(errorResponse.error?.message ?: defaultErrorMessage)
            } catch (e: Exception) {
                Resource.Error(defaultErrorMessage)
            }
        }
    } catch (e: IOException) {
        Resource.Error("网络错误，请检查网络连接")
    } catch (e: Exception) {
        Resource.Error("$defaultErrorMessage: ${e.message}")
    }
}

/**
 * 处理旧式API响应的扩展函数（针对不同类型的响应）
 */
fun <T> Response<T>.toLegacyResource(defaultErrorMessage: String): Resource<T> {
    return try {
        if (isSuccessful) {
            val body = body()
            if (body != null) {
                Resource.Success(body)
            } else {
                Resource.Error(defaultErrorMessage)
            }
        } else {
            Resource.Error("$defaultErrorMessage: ${message()}")
        }
    } catch (e: Exception) {
        Resource.Error("$defaultErrorMessage: ${e.message}")
    }
}