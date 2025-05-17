package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName

/**
 * ProductAbstract data model class
 * Simplified product information for list displays, reducing data transfer
 */
data class ProductAbstract(
    @SerializedName("id") val id: String, // Product ID
    @SerializedName("title") val title: String, // Product title
    @SerializedName("price") val price: Double, // Product price
    @SerializedName("images") val image: String, // Product image URL
    @SerializedName("status") val status: ProductStatus, // Product status
    @SerializedName("condition") val condition: String? = null, // Product condition
    @SerializedName("seller") val seller: UserAbstract? = null // Seller information
)
