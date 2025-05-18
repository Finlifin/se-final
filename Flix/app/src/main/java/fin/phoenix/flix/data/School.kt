package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName

/**
 * 学校数据类
 */
data class School(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String? = null,
    @SerializedName("city") val city: String? = null
)

/**
 * 校区数据类
 */
data class Campus(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("address") val address: String? = null
)