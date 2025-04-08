package fin.phoenix.flix.ui.message.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.TextMessageContent
import fin.phoenix.flix.ui.common.UserAvatar
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 接收者（聊天对象）的消息气泡
 */
@Composable
fun ReceiverMessageBubble(message: Message) {
    val content = message.content as? TextMessageContent ?: return
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(message.insertedAt)
    
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(start = 8.dp, end = 60.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            // 用户头像
            UserAvatar(
                avatarUrl = message.sender?.avatarUrl,
                size = 40.dp,
                placeholder = message.sender?.userName?.firstOrNull()?.toString() ?: "?"
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 消息气泡
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = content.text,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // 时间
            Text(
                text = timeString,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
        
        // 显示用户名，如果是第一次出现
        if (message.sender?.userName != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.sender?.userName ?: "未知用户",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 56.dp)
            )
        }
        
        // 如果是商品询问，显示商品信息
        val itemId = (message.content as? TextMessageContent)?.itemId
        if (itemId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(start = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEF6FF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "查看商品详情 →",
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp
                )
            }
        }
    }
}