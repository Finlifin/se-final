package fin.phoenix.flix.ui.message.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.TextMessageContent
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.common.UserAvatar
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 发送者（当前用户）的消息气泡
 */
@Composable
fun SenderMessageBubble(message: Message) {
    val content = message.content as? TextMessageContent ?: return
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(message.insertedAt)
    
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(start = 60.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            // 时间
            Text(
                text = timeString,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
            
            // 消息气泡
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(RoseRed.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = content.text,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 用户头像
            UserAvatar(
                avatarUrl = message.sender?.avatarUrl,
                size = 40.dp,
                placeholder = message.sender?.userName?.firstOrNull()?.toString() ?: "Me"
            )
        }
        
        // 消息状态（已发送/已读/发送中）
        if (message.id.startsWith("local-") || message.status == "pending") {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "发送中...",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 56.dp)
            )
        } else if (message.status == "read") {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "已读",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 56.dp)
            )
        } else if (message.status == "sent" || message.status == "unread") {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "已发送",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 56.dp)
            )
        }
    }
}