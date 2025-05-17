package fin.phoenix.flix.util

//const val IMAGE_BASE_URL = "http://192.168.31.117:5000/api/v1/images/"
const val IMAGE_BASE_URL = "http://10.70.141.134:5000/api/v1/images/"
fun imageUrl(image: String): String = "$IMAGE_BASE_URL$image"