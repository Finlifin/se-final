package fin.phoenix.flix.ui.message.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessagePreview
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.TextMessageContent
import fin.phoenix.flix.ui.colors.RoseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 会话列表项组件
 */
@Composable
fun ConversationItem(
    conversation: MessagePreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            BadgedBox(
                badge = {
                    if (conversation.unreadCount > 0) {
                        Badge(
                            containerColor = RoseRed
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            ) {
                Box {
                    if (conversation.messageType == MessageTypes.SYSTEM_NOTIFICATION || 
                        conversation.messageType == MessageTypes.SYSTEM_ANNOUNCEMENT
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(RoseRed.copy(alpha = 0.1f), CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "系统",
                                color = RoseRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = conversation.user.avatarUrl ?: "https://randomuser.me/api/portraits/lego/1.jpg"
                            ),
                            contentDescription = conversation.user.userName,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.user.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatMessageTime(conversation.timestamp),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = conversation.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = if (conversation.unreadCount > 0) Color.Black else Color.Gray
                )
            }
        }
    }
}

/**
 * 消息气泡组件 - 发送者
 */
@Composable
fun SenderMessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val content = message.content as? TextMessageContent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row {
            Spacer(modifier = Modifier.weight(0.15f))
            
            Box(
                modifier = Modifier
                    .weight(0.85f)
                    .clip(RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp))
                    .background(RoseRed.copy(alpha = 0.7f))
                    .padding(12.dp)
            ) {
                Text(
                    text = content?.text ?: "无内容",
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = formatMessageTime(message.insertedAt),
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

/**
 * 消息气泡组件 - 接收者
 */
@Composable
fun ReceiverMessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val content = message.content as? TextMessageContent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Avatar
            Image(
                painter = rememberAsyncImagePainter(
                    model = message.sender?.avatarUrl ?: "https://randomuser.me/api/portraits/lego/1.jpg"
                ),
                contentDescription = message.sender?.userName ?: "Unknown User",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = message.sender?.userName ?: "Unknown User",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp))
                        .background(Color(0xFFF0F0F0))
                        .padding(12.dp)
                ) {
                    Text(
                        text = content?.text ?: "无内容",
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatMessageTime(message.insertedAt),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * 系统消息组件
 */
@Composable
fun SystemMessageItem(
    message: Message,
    modifier: Modifier = Modifier
) {
    val content = message.content as? TextMessageContent
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = content?.text ?: "系统消息",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 格式化消息时间
 */
@Composable
fun formatMessageTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 30 -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date)
        days > 0 -> "${days}天前"
        hours > 0 -> "${hours}小时前"
        minutes > 0 -> "${minutes}分钟前"
        else -> "刚刚"
    }
}