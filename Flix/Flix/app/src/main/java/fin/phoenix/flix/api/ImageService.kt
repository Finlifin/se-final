package fin.phoenix.flix.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * 图片API服务接口
 */
interface ImageService {
    @Multipart
    @POST("upload")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<ImageUploadResponse>

    @Multipart
    @POST("upload/multiple")
    suspend fun uploadMultipleImages(@Part images: List<MultipartBody.Part>): Response<MultipleImageUploadResponse>
}

/**
 * 图片上传响应
 */
data class ImageUploadResponse(
    val success: Boolean, val url: String? = null
)

/**
 * 多图片上传响应
 */
data class MultipleImageUploadResponse(
    val success: Boolean, val imageUrls: List<String> = emptyList()
)
