package fin.phoenix.flix.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import fin.phoenix.flix.api.ErrorResponse
import fin.phoenix.flix.api.ImageService
import fin.phoenix.flix.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * 图片仓库类，处理图片上传相关的业务逻辑
 */
class ImageRepository(private val context: Context) {

    private val imageService = RetrofitClient.createService(ImageService::class.java, context, true)
    /**
     * 上传单张图片
     */
    suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 将Uri转换为File
            val file = uriToFile(imageUri)
                ?: return@withContext Result.failure(IOException("无法读取图片文件"))

            // 获取MIME类型
            val mimeType = getMimeType(file) ?: "image/jpeg"

            // 创建MultipartBody.Part
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            // 修改字段名从"image"为"file"以匹配服务器期望的字段名
            val imagePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            // 执行API请求
            val response = imageService.uploadImage(imagePart)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                return@withContext Result.failure(
                    IOException("上传失败，响应码: ${errorResponse.error.code}, 消息: ${errorResponse.error.message}")
                )
            }

            val responseBody =
                response.body() ?: return@withContext Result.failure(IOException("响应体为空"))

            if (!responseBody.success) {
                return@withContext Result.failure(IOException("上传失败"))
            }

            val imageUrl = responseBody.url
                ?: return@withContext Result.failure(IOException("返回的图片URL为空"))

            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // 清理临时文件
            cleanupTempFiles()
        }
    }

    /**
     * 上传多张图片
     */
    suspend fun uploadImages(imageUris: List<Uri>): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                if (imageUris.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                // 逐个上传方式
                val imageUrls = mutableListOf<String>()

                for (uri in imageUris) {
                    val result = uploadImage(uri)
                    if (result.isSuccess) {
                        imageUrls.add(result.getOrThrow())
                    } else {
                        return@withContext Result.failure(
                            result.exceptionOrNull() ?: IOException("上传图片失败")
                        )
                    }
                }

                Result.success(imageUrls)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 将Uri转换为File
     * @param uri 图片Uri
     * @return 转换后的File，失败返回null
     */
    private fun uriToFile(uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "${UUID.randomUUID()}.jpg"
        val tempFile = File(context.cacheDir, fileName)

        try {
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        } catch (e: Exception) {
            return null
        } finally {
            inputStream.close()
        }

        return tempFile
    }

    /**
     * 获取文件的MIME类型
     */
    private fun getMimeType(file: File): String? {
        val extension = file.extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    /**
     * 清理临时文件
     */
    private fun cleanupTempFiles() {
        val tempDir = context.cacheDir
        val currentTime = System.currentTimeMillis()
        val oneHourInMillis = 60 * 60 * 1000

        tempDir.listFiles()?.forEach { file ->
            if (currentTime - file.lastModified() > oneHourInMillis) {
                file.delete()
            }
        }
    }
}
