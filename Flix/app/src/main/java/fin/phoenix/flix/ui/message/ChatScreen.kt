package fin.phoenix.flix.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fin.phoenix.flix.api.ConnectionState
import fin.phoenix.flix.data.ContentTypes
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.formatTime
import fin.phoenix.flix.util.imageUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController, partnerUserId: String
) {
    val context = LocalContext.current
    val viewModel: MessageViewModel = viewModel()
    val chatState by viewModel.chatState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var message by remember { mutableStateOf("") }
    var showImagePicker by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentUser by UserManager.getInstance(context).currentUser.collectAsState()

    // 图片选择器
//    val imagePicker = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let {
//            viewModel.sendMessage(
//                conversationId = partnerUserId,
//                content = listOf(MessageContentItem("image", it.toString()))
//            )
//            showImagePicker = false
//        }
//    }

    // 加载会话信息
    LaunchedEffect(partnerUserId) {
        viewModel.loadChat(partnerUserId)
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(currentUser?.userName ?: "用户")
                if (connectionState != ConnectionState.CONNECTED) {
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTING -> "正在连接..."
                            ConnectionState.DISCONNECTED -> "连接已断开"
                            ConnectionState.CONNECTION_ERROR -> "连接失败"
                            else -> ""
                        }, fontSize = 12.sp, color = Color.Gray
                    )
                }
            }
        }, navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        })
    }, bottomBar = {
        Surface(
            modifier = Modifier.fillMaxWidth(), tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
//                    IconButton(onClick = { imagePicker.launch("image/*") }) {
//                        Icon(Icons.Default.Image, contentDescription = "发送图片")
//                    }

                TextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (message.isNotBlank()) {
                            viewModel.sendMessage(
                                sender = currentUser!!.uid,
                                conversationId = partnerUserId,
                                content = listOf(MessageContentItem(ContentTypes.TEXT, message))
                            )
                            message = ""
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (message.isNotBlank()) {
                            viewModel.sendMessage(
                                sender = currentUser!!.uid,
                                conversationId = partnerUserId,
                                content = listOf(MessageContentItem(ContentTypes.TEXT, message))
                            )
                            message = ""
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                }
            }
        }
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when (val state = chatState) {
                is MessageViewModel.ChatState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), color = RoseRed
                    )
                }

                is MessageViewModel.ChatState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages) { message ->
                            MessageItem(
                                message = message,
                                isOutgoing = message.senderId == currentUser?.uid,
                                onWithdrawClick = { showWithdrawDialog = message })
                        }
                    }

                    // 显示错误提示
                    state.error?.let { error ->
                        Snackbar(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomCenter),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Text(error)
                        }
                    }
                }

                is MessageViewModel.ChatState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message, color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadChat(partnerUserId) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        // 撤回消息确认对话框
        showWithdrawDialog?.let { message ->
            AlertDialog(
                onDismissRequest = { showWithdrawDialog = null },
                title = { Text("撤回消息") },
                text = { Text("确定要撤回这条消息吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.withdrawMessage(message.id)
                            showWithdrawDialog = null
                        }) {
                        Text("撤回")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWithdrawDialog = null }) {
                        Text("取消")
                    }
                })
        }
    }
}

@Composable
fun MessageItem(
    message: Message, isOutgoing: Boolean, onWithdrawClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // 时间戳
        Text(
            text = formatTime(message.serverTimestamp ?: message.clientTimestamp),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.Top, modifier = Modifier.padding(
                start = if (isOutgoing) 64.dp else 0.dp, end = if (isOutgoing) 0.dp else 64.dp
            )
        ) {
            if (!isOutgoing) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        // TODO
                        .data(imageUrl(message.sender)).crossfade(true).build(),
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
                    topStart = if (isOutgoing) 8.dp else 0.dp,
                    topEnd = if (isOutgoing) 0.dp else 8.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                ), color = if (isOutgoing) RoseRed else Color.White
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    for (content in message.content) {
                        when (content.type) {
                            "text" -> {
                                Text(
                                    text = content.payload as String,
                                    color = if (isOutgoing) Color.White else Color.Black
                                )
                            }

                            "image" -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(content.payload).crossfade(true).build(),
                                    contentDescription = "图片消息",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            else -> {
                                Text(
                                    text = "[不支持的消息类型]",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (message.status == "withdrawn") {
                        Text(
                            text = "消息已撤回",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else if (message.errorMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "发送失败",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "发送失败，点击重试", color = Color.Red, fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (isOutgoing && message.status != "withdrawn") {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onWithdrawClick, modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "撤回消息",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}