package fin.phoenix.flix.ui.message.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fin.phoenix.flix.data.MessagePreview
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.common.UserAvatar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 消息预览列表项
 */
@Composable
fun ConversationItem(
    preview: MessagePreview,
    onClick: () -> Unit
) {
    val timeString = formatTime(preview.timestamp)
    val isSystemMessage = preview.messageType == MessageTypes.SYSTEM_NOTIFICATION || 
            preview.messageType == MessageTypes.SYSTEM_ANNOUNCEMENT
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            UserAvatar(
                avatarUrl = preview.user.avatarUrl,
                size = 56.dp,
                placeholder = if (isSystemMessage) "S" else preview.user.userName.firstOrNull()?.toString() ?: "?"
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 消息内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 用户名和时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preview.user.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 消息预览
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (preview.lastSenderId == "system" || isSystemMessage) {
                            preview.lastMessage
                        } else {
                            buildMessagePreview(preview)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // 未读消息数
                    if (preview.unreadCount > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(RoseRed)
                        ) {
                            Text(
                                text = if (preview.unreadCount > 99) "99+" else preview.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 格式化消息时间
 */
private fun formatTime(date: Date): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }
    
    return when {
        // 今天
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        // 昨天
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "昨天"
        }
        // 同一年
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
        }
        // 不同年
        else -> {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        }
    }
}

/**
 * 构建消息预览文本
 */
private fun buildMessagePreview(preview: MessagePreview): String {
    return if (preview.lastSenderId == preview.user.uid) {
        preview.lastMessage
    } else {
        "我: ${preview.lastMessage}"
    }
}

/**
 * 消息预览列表为空时显示的内容
 */
@Composable
fun EmptyConversations() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "没有消息",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "您的消息将显示在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}