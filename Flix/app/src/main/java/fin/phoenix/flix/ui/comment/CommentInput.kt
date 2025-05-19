package fin.phoenix.flix.ui.comment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fin.phoenix.flix.ui.colors.RoseRed

/**
 * 评论输入底部栏
 */
@Composable
fun CommentInputBar(
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "写下你的评论",
) {
    Surface(
        modifier = modifier.fillMaxWidth(), tonalElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onFocus() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) {
                Text(
                    text = hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 评论输入面板
 */
@Composable
fun CommentInputPanel(
    inputState: CommentInputState,
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // 当进入回复模式时自动请求焦点
    LaunchedEffect(inputState) {
        if (inputState is CommentInputState.ForReply) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        tonalElevation = 4.dp, modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 显示回复信息
            if (inputState is CommentInputState.ForReply) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "回复 ${inputState.replyingToUsername}：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消回复",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 评论输入框
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = {
                    Text(
                        text = if (inputState is CommentInputState.ForReply) "回复 ${inputState.replyingToUsername}"
                        else "写下你的评论"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (commentText.isNotBlank()) {
                            onSubmit(commentText)
                            commentText = ""
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }),
                trailingIcon = {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .width(24.dp)
                                .height(24.dp),
                            color = RoseRed,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onSubmit(commentText)
                                    commentText = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }, enabled = commentText.isNotBlank() && !isSubmitting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "发送",
                                tint = if (commentText.isNotBlank()) RoseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = false,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 底部操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        commentText = ""
                        onCancel()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }) {
                    Text("取消")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSubmit(commentText)
                            commentText = ""
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }, enabled = commentText.isNotBlank() && !isSubmitting
                ) {
                    Text(
                        text = if (inputState is CommentInputState.ForReply) "回复" else "发布评论"
                    )
                }
            }
        }
    }
}

/**
 * 评论操作状态显示条
 */
@Composable
fun CommentOperationStatus(
    operation: CommentOperation, onDismiss: () -> Unit, modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = operation !is CommentOperation.Idle, modifier = modifier
    ) {
        when (operation) {
            is CommentOperation.InProgress -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = RoseRed
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = operation.message, style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            is CommentOperation.Success -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = operation.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss) {
                            Text("关闭")
                        }
                    }
                }
            }

            is CommentOperation.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = operation.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss) {
                            Text("关闭")
                        }
                    }
                }
            }

            else -> {}
        }
    }
}