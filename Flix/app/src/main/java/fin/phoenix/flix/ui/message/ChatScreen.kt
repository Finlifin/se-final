package fin.phoenix.flix.ui.message

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
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
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.data.ContentTypes
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.Order
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.repository.ImageRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.home.ProductCard
import fin.phoenix.flix.ui.product.CommentAsMessage
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.formatTime
import fin.phoenix.flix.util.imageUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController, participantId: String
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel()
    val chatState by viewModel.chatState.observeAsState()
    val connectionState by viewModel.connectionState.observeAsState()
    var message by remember { mutableStateOf("") }
    var showWithdrawDialog by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentUser by UserManager.getInstance(context).currentUser.observeAsState()
    var isUploading by remember { mutableStateOf(false) }

    // 创建ImageRepository实例用于图片上传
    val imageRepository = remember { ImageRepository(context) }

    // 获取对方用户信息
    var otherUser by remember { mutableStateOf<UserAbstract?>(null) }
    val isSystemNotification = participantId == "server"
    val profileRepository = remember { ProfileRepository(context) }

    // 图片选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 显示上传中状态
            isUploading = true

            // 在后台上传图片
            coroutineScope.launch {
                try {
                    // 上传图片到服务器
                    val result = imageRepository.uploadImage(it)

                    if (result.isSuccess) {
                        // 获取上传成功后的图片URL
                        val imageUrl = result.getOrThrow()

                        // 发送图片消息
                        viewModel.sendMessage(
                            receiverId = participantId,
                            content = listOf(MessageContentItem(ContentTypes.IMAGE, imageUrl))
                        )
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "图片上传失败"
                        Log.e("ChatScreen", "上传图片失败: $error")
                    }
                } catch (e: Exception) {
                    Log.e("ChatScreen", "上传图片出错", e)
                } finally {
                    isUploading = false
                }
            }
        }
    }

    LaunchedEffect(participantId) {
        if (isSystemNotification) {
            // 系统通知，加载系统通知消息
            viewModel.loadSystemMessages()
        } else {
            // 正常的用户对话，加载对方用户信息
            val result = profileRepository.getUserAbstract(participantId)
            if (result is Resource.Success) {
                otherUser = result.data
            }
            // 加载会话消息
            viewModel.loadChatByParticipantId(participantId)
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(
                    if (isSystemNotification) "系统通知"
                    else otherUser?.userName ?: "会话"
                )
                if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.JOINED) {
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
        }, actions = {
            // 只有非系统通知才显示聊天设置按钮
            if (!isSystemNotification) {
                IconButton(onClick = {
                    // 当chatState可用且为Success状态时，跳转到聊天设置页面
                    if (chatState is ChatViewModel.ChatState.Success) {
                        navController.navigate("/messages/settings/${(chatState as ChatViewModel.ChatState.Success).conversation.participantId}")
                    }
                }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "聊天设置")
                }
            }
        })
    }, bottomBar = {
        // 只有非系统通知才显示消息输入框
        if (!isSystemNotification) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(), tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "发送图片")
                    }

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
                                    receiverId = participantId,
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
                                    receiverId = participantId,
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
        }
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when (val state = chatState) {
                is ChatViewModel.ChatState.Loading, null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), color = RoseRed
                    )
                }

                is ChatViewModel.ChatState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages) { message ->
                            MessageItem(
                                message = message.apply {
                                    if (message.senderId != currentUser?.uid) {
                                        sender = otherUser
                                    }
                                },
                                isOutgoing = message.senderId == currentUser?.uid,
                                onWithdrawClick = {
                                    if (!isSystemNotification) {
                                        showWithdrawDialog = message
                                    }
                                },
                                navController = navController
                            )
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

                is ChatViewModel.ChatState.Error -> {
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
                            onClick = {
                                if (isSystemNotification) {
                                    viewModel.loadSystemMessages()
                                } else {
                                    viewModel.loadChatByParticipantId(participantId)
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
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
//                            viewModel.withdrawMessage(message.id)
//                            showWithdrawDialog = null
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
    message: Message, isOutgoing: Boolean, onWithdrawClick: () -> Unit, navController: NavController
) {
    LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // 时间戳
        Text(
            text = formatTime(message.createdAt),
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
                        .data(imageUrl(message.sender?.avatarUrl))
                        .crossfade(true).build(),
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
                ),
                color = if (isOutgoing) RoseRed else Color.White,
                // 添加阴影效果
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        // 添加透明背景
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        .data(imageUrl(content.payload as String)).crossfade(true)
                                        .build(),
                                    contentDescription = "图片消息",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            "comment" -> {
                                // 使用CommentAsMessage组件处理评论类型消息
                                when (content.payload) {
                                    is Comment -> {
                                        // 直接是Comment对象的情况
                                        val comment = content.payload as Comment
                                        fin.phoenix.flix.ui.product.CommentAsMessage(
                                            comment = comment,
                                            isReply = isOutgoing,
                                            onClick = { /* 处理点击事件 */ },
                                            onLikeClick = { /* 处理点赞事件 */ },
                                            navController = navController
                                        )
                                    }
                                    is Map<*, *> -> {
                                        // 评论数据是Map的情况
                                        val commentMap = content.payload as Map<*, *>
                                        val commentText = commentMap["content"] as? String ?: "[评论内容]"
                                        Text(
                                            text = "评论: $commentText",
                                            color = if (isOutgoing) Color.White else Color.Black
                                        )
                                    }
                                    else -> {
                                        // 其他情况，简单显示文本
                                        val commentText = if (content.payload is String) {
                                            content.payload as String
                                        } else {
                                            "[评论]"
                                        }
                                        Text(
                                            text = commentText,
                                            color = if (isOutgoing) Color.White else Color.Black
                                        )
                                    }
                                }
                            }

                            "product" -> {
                                val product = content.payload as? Product
                                if (product != null) {
                                    ProductCard(product = product.toAbstract(), onClick = {
                                        navController.navigate("/product/${product.id}")
                                    })
                                } else {
                                    Text(
                                        text = "[商品信息]",
                                        color = if (isOutgoing) Color.White else Color.Gray,
                                    )
                                }
                            }

                            "order" -> {
                                val payload = content.payload as? Order
                                if (payload != null) {
                                    if (payload.orderType == "recharge") {
                                        Column {
                                            Text(
                                                text = "充值成功，共充值 ${payload.price} 元",

                                                color = if (isOutgoing) Color.White else Color.Gray
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .clickable {
                                                    navController.navigate("/orders/${payload.orderId}")
                                                },
                                            color = Color(0xFFF5F5F5),
                                            // 为订单卡片添加阴影
                                            shadowElevation = 2.dp
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .background(Color.Transparent)
                                            ) {
                                                Text(
                                                    text = "[订单]",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )

                                                Row(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = payload.productId,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            maxLines = 2,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "¥${payload.price}",
                                                                color = RoseRed,
                                                                fontSize = 14.sp,
                                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                            )

                                                            Spacer(modifier = Modifier.width(8.dp))

                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = getOrderStatusColor(payload.status.toString()).first
                                                            ) {
                                                                Text(
                                                                    text = getOrderStatusText(payload.status.toString()),
                                                                    color = getOrderStatusColor(payload.status.toString()).second,
                                                                    fontSize = 12.sp,
                                                                    modifier = Modifier.padding(
                                                                        horizontal = 4.dp,
                                                                        vertical = 2.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    Text(
                                        text = "[订单信息]",
                                        color = if (isOutgoing) Color.White else Color.Gray
                                    )
                                }
                            }

                            "favorite" -> {
                                val payload = content.payload as? Map<*, *>
                                if (payload != null) {
                                    val user = payload["user"] as? Map<*, *>
                                    val product = payload["product"] as? Map<*, *>

                                    if (user != null && product != null) {
                                        val userName = user["userName"] as? String ?: "未知用户"
                                        val productName = product["title"] as? String ?: "未知商品"
                                        val productImage = product["image"] as? String

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth(0.8f),
                                            color = Color(0xFFF5F5F5)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "[收藏信息]",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )

                                                Row(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (productImage != null) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(
                                                                LocalContext.current
                                                            ).data(productImage).crossfade(true)
                                                                .build(),
                                                            contentDescription = "商品图片",
                                                            modifier = Modifier
                                                                .size(60.dp)
                                                                .clip(RoundedCornerShape(4.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }

                                                    Column {
                                                        Text(
                                                            text = "$userName 收藏了商品",
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )

                                                        Text(
                                                            text = productName,
                                                            maxLines = 2,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "[收藏信息]",
                                            color = if (isOutgoing) Color.White else Color.Gray
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "[收藏信息]",
                                        color = if (isOutgoing) Color.White else Color.Gray
                                    )
                                }
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
        }
    }
}

fun getOrderStatusColor(status: String): Pair<Color, Color> {
    return when (status.lowercase()) {
        "pending" -> Pair(Color(0xFFF9F9F9), Color(0xFF666666))
        "payment_pending" -> Pair(Color(0xFFFFF7E6), Color(0xFFD46B08))
        "paid" -> Pair(Color(0xFFE6F7FF), Color(0xFF1890FF))
        "shipping" -> Pair(Color(0xFFE6F7FF), Color(0xFF1890FF))
        "completed" -> Pair(Color(0xFFF6FFED), Color(0xFF52C41A))
        "cancelled" -> Pair(Color(0xFFFFF1F0), Color(0xFFF5222D))
        "refunded" -> Pair(Color(0xFFFFF1F0), Color(0xFFF5222D))
        else -> Pair(Color(0xFFF9F9F9), Color(0xFF666666))
    }
}

fun getOrderStatusText(status: String): String {
    return when (status.lowercase()) {
        "pending" -> "待处理"
        "payment_pending" -> "待支付"
        "paid" -> "已支付"
        "shipping" -> "配送中"
        "completed" -> "已完成"
        "cancelled" -> "已取消"
        "refunded" -> "已退款"
        else -> status
    }
}