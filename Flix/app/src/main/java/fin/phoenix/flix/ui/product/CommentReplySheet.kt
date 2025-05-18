package fin.phoenix.flix.ui.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.data.CommentContext
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

/**
 * 评论回复底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentReplySheet(
    viewModel: CommentViewModel,
    onDismiss: () -> Unit
) {
    val selectedComment by viewModel.selectedComment.observeAsState()
    val commentContext by viewModel.commentContext.observeAsState(Resource.Loading)
    val newReplyContent by viewModel.newReplyContent.observeAsState("")
    val isSubmitting by viewModel.isSubmitting.observeAsState(false)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 优化生命周期管理 - 添加显式的 DisposableEffect 以确保清理
    DisposableEffect(Unit) {
        onDispose {
            // 在组件销毁时确保所有操作都被取消
            viewModel.updateNewReplyContent("")
        }
    }
    
    if (selectedComment != null) {
        ModalBottomSheet(
            onDismissRequest = {
                // 确保在关闭表单时清理资源
                scope.launch {
                    try {
                        // 尝试收起底部表单
                        sheetState.hide()
                        onDismiss()
                    } catch (e: Exception) {
                        // 如果发生异常，强制关闭
                        onDismiss()
                    }
                }
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "评论详情",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = {
                        // 确保在点击关闭按钮时清理资源
                        scope.launch {
                            try {
                                sheetState.hide()
                                onDismiss()
                            } catch (e: Exception) {
                                onDismiss()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 原始评论
                selectedComment?.let { comment ->
                    CommentCard(
                        comment = comment,
                        showReplies = true,
                        onLikeClick = {
                            if (comment.isLiked) {
                                viewModel.unlikeComment(comment)
                            } else {
                                viewModel.likeComment(comment)
                            }
                        }
                    )
                }
                
                // 回复列表
                when (commentContext) {
                    is Resource.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = RoseRed)
                        }
                    }
                    
                    is Resource.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "加载回复失败: ${(commentContext as Resource.Error).message}",
                                color = Color.Red
                            )
                        }
                    }
                    
                    is Resource.Success -> {
                        val context = (commentContext as Resource.Success<CommentContext>).data
                        val replies = context.replies
                        
                        if (replies.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无回复，快来发表第一条回复吧",
                                    color = Color.Gray
                                )
                            }
                        } else {
                            Text(
                                text = "全部回复(${replies.size})",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                            ) {
                                items(replies) { reply ->
                                    // 显示回复评论，不需要点击功能
                                    CommentCard(
                                        comment = reply,
                                        showReplies = true,
                                        onLikeClick = {
                                            if (reply.isLiked) {
                                                viewModel.unlikeComment(reply)
                                            } else {
                                                viewModel.likeComment(reply)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 回复输入框
                OutlinedTextField(
                    value = newReplyContent,
                    onValueChange = { viewModel.updateNewReplyContent(it) },
                    placeholder = { Text("回复评论...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newReplyContent.isNotBlank() && !isSubmitting && selectedComment != null) {
                                    viewModel.submitReply(selectedComment!!.id) {
                                        // 回复成功后，滚动到底部
                                        scope.launch {
                                            val context = (commentContext as? Resource.Success<CommentContext>)?.data
                                            if (context?.replies?.isNotEmpty() == true) {
                                                listState.animateScrollToItem(context.replies.size - 1)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = newReplyContent.isNotBlank() && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = RoseRed,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "发送回复",
                                    tint = if (newReplyContent.isNotBlank()) RoseRed else Color.Gray
                                )
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}