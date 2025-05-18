package fin.phoenix.flix.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl

/**
 * 用户头像组件
 * 可以显示网络图片或生成字母头像
 */
@Composable
fun UserAvatar(
    avatarUrl: String?, 
    size: Dp = 40.dp,
    placeholder: String = "?",
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    if (!avatarUrl.isNullOrEmpty()) {
        // 显示网络图片
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl(avatarUrl))
                .crossfade(true)
                .build(),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        // 显示字母头像
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (placeholder == "Me") RoseRed else Color(0xFF2563EB)
                )
        ) {
            Text(
                text = placeholder.take(1).uppercase(),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}