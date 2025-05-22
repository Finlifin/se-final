package fin.phoenix.flix.ui.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.ui.colors.RoseRed

/**
 * 评论列表组件
 */
@Composable
fun CommentList(
    comments: List<Comment>,
    currentUserId: String?,
    isLoading: Boolean,
    onReplyClick: (Comment) -> Unit,
    onLikeClick: (Comment) -> Unit,
    onDeleteClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    emptyText: String = "暂无评论，来发表第一条评论吧！"
) {
    val listState = rememberLazyListState()

    // 检测是否滚动到末尾
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index != 0 && lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    // 当滚动到末尾时加载更多
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && comments.isNotEmpty()) {
            onLoadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (comments.isEmpty() && !isLoading) {
            // 空状态
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 评论列表
            LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = comments, key = { it.id }) { comment ->
                    CommentItem(
                        comment = comment,
                        currentUserId = currentUserId,
                        onReplyClick = onReplyClick,
                        onLikeClick = onLikeClick,
                        onDeleteClick = onDeleteClick,
                        onUserClick = onUserClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isLoading) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            CircularProgressIndicator(color = RoseRed)
                        }
                    }
                }
            }
        }

        // 加载状态
        if (isLoading && comments.isEmpty()) {
            CircularProgressIndicator(
                color = RoseRed, modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * 评论回复列表组件
 */
@Composable
fun CommentRepliesList(
    rootComment: Comment,
    replies: List<Comment>,
    currentUserId: String?,
    isLoading: Boolean,
    onReplyClick: (Comment) -> Unit,
    onLikeClick: (Comment) -> Unit,
    onDeleteClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()
        ) {
            // 根评论
            CommentItem(
                comment = rootComment,
                currentUserId = currentUserId,
                onReplyClick = onReplyClick,
                onLikeClick = onLikeClick,
                onDeleteClick = onDeleteClick,
                onUserClick = onUserClick,
                modifier = Modifier.padding(16.dp)
            )
        }

        // 回复列表标题
        Text(
            text = "全部回复 (${rootComment.repliesCount})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 回复列表
        CommentList(
            comments = replies,
            currentUserId = currentUserId,
            isLoading = isLoading,
            onReplyClick = onReplyClick,
            onLikeClick = onLikeClick,
            onDeleteClick = onDeleteClick,
            onUserClick = onUserClick,
            onLoadMore = onLoadMore,
            contentPadding = contentPadding,
            emptyText = "暂无回复",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 评论上下文组件 - 用于定位到特定评论
 */
@Composable
fun CommentContextView(
    rootComment: Comment,
    replies: List<Comment>,
    targetCommentId: String,
    currentUserId: String?,
    onReplyClick: (Comment) -> Unit,
    onLikeClick: (Comment) -> Unit,
    onDeleteClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()
        ) {
            // 根评论
            CommentItem(
                comment = rootComment.copy(isHighlighted = rootComment.id == targetCommentId),
                currentUserId = currentUserId,
                onReplyClick = onReplyClick,
                onLikeClick = onLikeClick,
                onDeleteClick = onDeleteClick,
                onUserClick = onUserClick,
                modifier = Modifier.padding(16.dp)
            )
        }

        // 回复列表标题
        Text(
            text = "全部回复 (${rootComment.repliesCount})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 回复列表
        LazyColumn(
            contentPadding = contentPadding, modifier = Modifier.weight(1f)
        ) {
            items(
                items = replies, key = { it.id }) { reply ->
                CommentItem(
                    comment = reply.copy(isHighlighted = reply.id == targetCommentId),
                    currentUserId = currentUserId,
                    isReply = true,
                    onReplyClick = onReplyClick,
                    onLikeClick = onLikeClick,
                    onDeleteClick = onDeleteClick,
                    onUserClick = onUserClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}