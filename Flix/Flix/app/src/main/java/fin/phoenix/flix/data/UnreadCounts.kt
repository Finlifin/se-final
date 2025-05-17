package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName

/**
 * 未读消息计数数据类
 */
data class UnreadCounts(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("chat") val chat: Int = 0,
    @SerializedName("system") val system: Int = 0,
    @SerializedName("order") val order: Int = 0,
    @SerializedName("payment") val payment: Int = 0
)