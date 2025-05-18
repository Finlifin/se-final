package fin.phoenix.flix.ui.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.imageUrl

/**
 * 评论卡片组件
 */
@Composable
fun CommentCard(
    comment: Comment,
    showReplies: Boolean = false,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {}
) {
    // 本地状态，用于立即更新UI
    var isLiked by remember { mutableStateOf(comment.isLiked) }
    var likesCount by remember { mutableStateOf(comment.likesCount) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.White)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        // 添加阴影效果
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color.Transparent) // 设置透明背景
        ) {
            // 评论头部信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 用户头像
                AsyncImage(
                    model = imageUrl(comment.user?.avatarUrl),
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 用户名和评论时间
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.user?.userName ?: "未知用户",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )

                    Text(
                        text = formatCommentTime(comment.createdAt.time),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // 点赞按钮，使用大拇指图标
                IconButton(onClick = { 
                    // 本地立即更新UI状态
                    isLiked = !isLiked
                    likesCount = if (isLiked) likesCount + 1 else likesCount - 1
                    // 调用API进行实际更新
                    onLikeClick() 
                }) {
                    val icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp
                    val tint = if (isLiked) RoseRed else Color.Gray

                    Icon(
                        imageVector = icon,
                        contentDescription = if (isLiked) "取消点赞" else "点赞",
                        tint = tint
                    )
                }
            }

            // 评论内容
            Text(
                text = comment.content,
                modifier = Modifier.padding(vertical = 8.dp),
                lineHeight = 22.sp
            )

            // 显示回复统计信息或部分回复内容
            if (comment.repliesCount > 0) {
                Surface(
                    modifier = Modifier.padding(top = 8.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${comment.repliesCount}条回复",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (!showReplies) {
                            Text(
                                text = "查看全部 >",
                                color = RoseRed,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable(onClick = onClick)
                            )
                        }
                    }
                }
            }

            // 底部信息栏，显示点赞数量
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${likesCount}人点赞", color = Color.Gray, fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * 使用MessageItem样式显示评论的组件
 * 这个组件可以复用ChatScreen.kt中的MessageItem风格
 */
@Composable
fun CommentAsMessage(
    comment: Comment,
    isReply: Boolean = false,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    navController: NavController
) {
    // 本地状态，用于立即更新UI
    var isLiked by remember { mutableStateOf(comment.isLiked) }
    var likesCount by remember { mutableStateOf(comment.likesCount) }
    
    // 将评论转换为消息格式显示
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isReply) Alignment.End else Alignment.Start
    ) {
        // 时间戳
        Text(
            text = formatCommentTime(comment.createdAt.time),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(
                start = if (isReply) 64.dp else 0.dp,
                end = if (isReply) 0.dp else 64.dp
            )
        ) {
            if (!isReply) {
                // 显示用户头像
                AsyncImage(
                    model = imageUrl(comment.user?.avatarUrl ?: "loading_avatar.png"),
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isReply) 8.dp else 0.dp,
                    topEnd = if (isReply) 0.dp else 8.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                ),
                color = if (isReply) RoseRed else Color.White,
                modifier = Modifier.clickable(onClick = onClick)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 评论内容
                    Text(
                        text = comment.content,
                        color = if (isReply) Color.White else Color.Black
                    )
                    
                    // 如果是回复，显示回复了谁
//                    if (isReply && comment.parentId != null) {
//                        Text(
//                            text = "回复: ${comment.parentComment?.user?.userName ?: "原评论"}",
//                            fontSize = 12.sp,
//                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
//                            color = if (isReply) Color.White.copy(alpha = 0.7f) else Color.Gray
//                        )
//                    }
                    
                    // 底部信息栏，显示点赞数量和点赞按钮
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${likesCount}人点赞",
                            color = if (isReply) Color.White.copy(alpha = 0.8f) else Color.Gray,
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { 
                                // 本地立即更新UI状态
                                isLiked = !isLiked
                                likesCount = if (isLiked) likesCount + 1 else likesCount - 1
                                // 调用API进行实际更新
                                onLikeClick() 
                            },
                            modifier = Modifier.size(18.dp)
                        ) {
                            val icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp
                            val tint = if (isReply) {
                                if (isLiked) Color.White else Color.White.copy(alpha = 0.8f)
                            } else {
                                if (isLiked) RoseRed else Color.Gray
                            }
                            
                            Icon(
                                imageVector = icon,
                                contentDescription = if (isLiked) "取消点赞" else "点赞",
                                tint = tint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 评论列表组件，显示商品下的所有根评论
 */
@Composable
fun CommentSection(
    viewModel: CommentViewModel, productId: String, onShowReplySheet: (Comment) -> Unit
) {
    val commentPagingItems = viewModel.getProductComments(productId).collectAsLazyPagingItems()
    val newCommentContent by viewModel.newCommentContent.observeAsState("")
    val isSubmitting by viewModel.isSubmitting.observeAsState(false)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 评论区标题
        Text(
            text = "商品评论",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 评论输入框
        OutlinedTextField(
            value = newCommentContent,
            onValueChange = { viewModel.updateNewCommentContent(it) },
            placeholder = { Text("说说你对这个商品的看法...") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (newCommentContent.isNotBlank() && !isSubmitting) {
                            viewModel.submitComment(productId) {
                                commentPagingItems.refresh()
                            }
                        }
                    }, enabled = newCommentContent.isNotBlank() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), color = RoseRed, strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送评论",
                            tint = if (newCommentContent.isNotBlank()) RoseRed else Color.Gray
                        )
                    }
                }
            })

        Spacer(modifier = Modifier.height(16.dp))

        // 评论列表
        when (commentPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RoseRed)
                }
            }

            is LoadState.Error -> {
                val error = (commentPagingItems.loadState.refresh as LoadState.Error).error
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "加载评论失败", color = Color.Gray, fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = error.localizedMessage ?: "未知错误", color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { commentPagingItems.refresh() }) {
                            Text("重试")
                        }
                    }
                }
            }

            is LoadState.NotLoading -> {
                if (commentPagingItems.itemCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无评论，快来发表第一条评论吧", color = Color.Gray
                        )
                    }
                } else {
                    // 直接列出评论列表，不包装在Box中
                    for (index in 0 until commentPagingItems.itemCount) {
                        val comment = commentPagingItems[index]
                        if (comment != null) {
                            CommentCard(
                                comment = comment,
                                onClick = { onShowReplySheet(comment) },
                                onLikeClick = {
                                    if (comment.isLiked) {
                                        viewModel.unlikeComment(comment)
                                    } else {
                                        viewModel.likeComment(comment)
                                    }
                                }
                            )
                        }
                    }

                    // 加载更多指示器
                    if (commentPagingItems.loadState.append is LoadState.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = RoseRed,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 格式化评论时间
 */
fun formatCommentTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp) / 1000  // 转换为秒级差异

    return when {
        diff < 60 -> "刚刚"
        diff < 60 * 60 -> "${diff / 60}分钟前"
        diff < 24 * 60 * 60 -> "${diff / (60 * 60)}小时前"
        diff < 7 * 24 * 60 * 60 -> "${diff / (24 * 60 * 60)}天前"
        else -> {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
            date
        }
    }
}