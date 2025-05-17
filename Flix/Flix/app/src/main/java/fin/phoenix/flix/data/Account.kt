package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName


data class Account(
    @SerializedName("id") val id: String,
    @SerializedName("phone_number") val phoneNumber: String
)
