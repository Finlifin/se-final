package fin.phoenix.flix.ui.comment

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.common.UserAvatar
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 评论项组件
 */
@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onReplyClick: (Comment) -> Unit,
    onLikeClick: (Comment) -> Unit,
    onDeleteClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isReply: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (comment.isHighlighted) {
                    Modifier.background(
                        Color(0xFFFFEBEE), RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(8.dp)
    ) {
        Column {
            // 用户信息区域
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                // 用户头像
                UserAvatar(
                    avatarUrl = comment.user?.avatarUrl,
                    size = 36.dp,
                    modifier = Modifier.clickable {
                        comment.user?.uid?.let { onUserClick(it) }
                    })

                Spacer(modifier = Modifier.width(8.dp))

                // 用户名和评论时间
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = comment.user?.userName ?: "用户已注销",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = formatDate(comment.createdAt.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 评论操作菜单
                if (currentUserId == comment.userId) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "更多操作"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete, contentDescription = "删除"
                                )
                            }, text = { Text("删除评论") }, onClick = {
                                onDeleteClick(comment.id)
                                showMenu = false
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 评论内容
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 44.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 评论操作区域
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp)
            ) {
                // 回复按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onReplyClick(comment) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "回复",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = if (comment.repliesCount > 0) "${comment.repliesCount}" else "回复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 点赞按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick(comment) }) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "点赞",
                        tint = if (comment.isLiked) RoseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = if (comment.likesCount > 0) "${comment.likesCount}" else "点赞",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (comment.isLiked) RoseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 对于根评论，显示"查看更多回复"按钮
            if (!isReply && comment.repliesCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .padding(start = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onReplyClick(comment) }
                        .padding(vertical = 4.dp)) {
                    Text(
                        text = "查看 ${comment.repliesCount} 条回复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 格式化日期为相对时间
 */
private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "刚刚"  // 1分钟内
        diff < 3600_000 -> "${diff / 60_000}分钟前"  // 1小时内
        diff < 86400_000 -> "${diff / 3600_000}小时前"  // 1天内
        diff < 604800_000 -> "${diff / 86400_000}天前"  // 1周内
        else -> {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.format(timestamp)
        }
    }
}