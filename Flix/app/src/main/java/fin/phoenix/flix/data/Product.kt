package fin.phoenix.flix.data

import com.google.gson.annotations.SerializedName

/**
 * Product status enum
 * Represents the current status of a product
 */
enum class ProductStatus {
    @SerializedName("available")
    AVAILABLE,     // Available for sale

    @SerializedName("sold")
    SOLD,          // Already sold

    @SerializedName("delete")
    DELETE,        // Deleted by user

    @SerializedName("removed")
    REMOVED        // Removed from listing
}

/**
 * Product data model class
 * Represents a product in the second-hand trading platform
 */
data class Product(
    @SerializedName("id") val id: String, // Product ID, unique identifier (primary key)
    @SerializedName("seller_id") val sellerId: String, // Seller ID, references User table
    @SerializedName("title") val title: String, // Product title
    @SerializedName("description") val description: String, // Product description
    @SerializedName("price") val price: Double, // Product price
    @SerializedName("images") val images: List<String>, // List of product image URLs
    @SerializedName("category") val category: String, // Product category
    @SerializedName("condition") val condition: String, // Product condition (e.g., "New", "90% new")
    @SerializedName("location") val location: String, // Product location/shipping location
    @SerializedName("post_time") val postTime: Long, // Product posting timestamp (milliseconds)
    @SerializedName("status") val status: ProductStatus, // Product status
    @SerializedName("view_count") val viewCount: Int = 0, // Product view count, default 0
    @SerializedName("favorite_count") val favoriteCount: Int = 0, // Product favorite count, default 0
    @SerializedName("tags") val tags: List<String> = emptyList(), // Product tags
    @SerializedName("campus_id") val campusId: String? = null, // Campus ID (可空)
    @SerializedName("available_delivery_methods") val availableDeliveryMethods: List<String> = listOf("express", "pickup") // Available delivery methods
) {
    fun toAbstract(): ProductAbstract {
        return ProductAbstract(
            id = id,
            title = title,
            price = price,
            image = images.firstOrNull() ?: "",
            condition = condition,
            status = status ?: ProductStatus.DELETE,
            campusId = campusId,
                         )
    }
}
