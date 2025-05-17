package fin.phoenix.flix.api


/**
 * 通用响应接口，包含成功标志和消息
 */
interface ApiResponse {
    val success: Boolean
    val message: String?
}


/**
 * API错误详情
 */
data class ErrorDetails(
    val code: String,
    val message: String
)

/**
 * 错误响应（用于解析errorBody）
 */
data class ErrorResponse(
    val success: Boolean,
    val error: ErrorDetails
)

/**
 * 通用API响应实现类，适用于服务端统一响应格式
 */
data class GenericApiResponse<T>(
    override val success: Boolean,
    override val message: String? = null,
    val data: T? = null,
    val error: ErrorDetails? = null
) : ApiResponse