package fin.phoenix.flix.ui.message

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.message.components.ReceiverMessageBubble
import fin.phoenix.flix.ui.message.components.SenderMessageBubble
import fin.phoenix.flix.ui.message.components.SystemMessageItem
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(navController: NavController, partnerUserId: String) {
    val context = LocalContext.current
    val viewModel: MessageViewModel = viewModel(
        factory = MessageViewModelFactory(context)
    )

    // 当前聊天状态
    val messagesState by viewModel.messagesState.collectAsState()
    val sendMessageState by viewModel.sendMessageState.collectAsState()

    var messageText by remember { mutableStateOf("") }

    // 滚动状态
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 加载与特定用户的消息历史
    LaunchedEffect(partnerUserId) {
        Toast.makeText(context, "加载与用户 $partnerUserId 的消息", Toast.LENGTH_SHORT).show()
        viewModel.loadMessages(partnerUserId)
    }

// 观察消息发送状态
    LaunchedEffect(sendMessageState) {
        when (sendMessageState) {
            is Resource.Success -> {
                messageText = ""
                viewModel.clearSendMessageState()

                // 滚动到底部
                coroutineScope.launch {
                    val messageCount = (messagesState as? Resource.Success)?.data?.size ?: 0
                    if (messageCount > 0) {
                        listState.scrollToItem(messageCount - 1)
                    }
                }
            }

            is Resource.Error -> {
                // 显示错误提示
                Toast.makeText(
                    context,
                    "发送失败: ${(sendMessageState as Resource.Error).message}",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.clearSendMessageState()
            }

            else -> {} // 处理加载状态或空状态
        }
    }

    // 在离开页面时清除当前聊天状态
    DisposableEffect(key1 = Unit) {
        onDispose {
            viewModel.clearCurrentChat()
        }
    }

    // 聊天伙伴的名称
    var partnerName by remember { mutableStateOf("") }

    // 当消息加载成功时，更新伙伴名称
    LaunchedEffect(messagesState) {
        if (messagesState is Resource.Success) {
            val messages = (messagesState as Resource.Success<List<Message>>).data
            if (messages.isNotEmpty()) {
                messages.firstOrNull { it.senderId == partnerUserId && it.sender != null }?.let {
                    partnerName = it.sender?.userName ?: "wtf"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                Text(
                    text = partnerName.ifEmpty { "聊天" })
            }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
            )
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            // 消息列表
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = messagesState) {
                    is Resource.Loading -> {
                        // 加载状态
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = RoseRed, modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    is Resource.Error -> {
                        // 错误状态
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "加载失败",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    is Resource.Success -> {
                        val messages = state.data

                        if (messages.isEmpty()) {
                            // 空数据状态
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "开始聊天吧",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                reverseLayout = false
                            ) {
                                items(messages) { message ->
                                    when {
                                        message.messageType == MessageTypes.SYSTEM_NOTIFICATION -> {
                                            SystemMessageItem(message = message)
                                        }

                                        message.senderId == partnerUserId -> {
                                            ReceiverMessageBubble(message = message)
                                        }

                                        else -> {
                                            SenderMessageBubble(message = message)
                                        }
                                    }
                                }
                            }

                            // 初始滚动到底部
                            LaunchedEffect(messages.size) {
                                if (messages.isNotEmpty()) {
                                    listState.scrollToItem(messages.size - 1)
                                }
                            }
                        }
                    }
                }
            }

            // 消息输入区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding(), verticalAlignment = Alignment.CenterVertically
            ) {
                // 消息输入框
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("输入消息...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )

                // 发送按钮
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(partnerUserId, messageText)
                            keyboardController?.hide()
                        }
                    }, modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (messageText.isBlank()) Color.Gray.copy(alpha = 0.3f) else RoseRed,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = Color.White
                    )
                }
            }
        }
    }
}