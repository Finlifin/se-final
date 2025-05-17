package fin.phoenix.flix.ui.message

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fin.phoenix.flix.api.ConnectionState
import fin.phoenix.flix.data.Conversation
import fin.phoenix.flix.data.UserAbstract
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.data.repository.MessageRepository
import fin.phoenix.flix.repository.ProfileRepository
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.formatTime
import fin.phoenix.flix.util.imageUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(navController: NavController) {
    val viewModel: MessageViewModel = viewModel()
    val conversationListState by viewModel.conversationListState.observeAsState()
    val connectionState by viewModel.connectionState.observeAsState()

    // 获取UserManager实例和当前用户ID
    val context = LocalContext.current
    val userManager = UserManager.getInstance(context)
    val currentUserId by userManager.currentUserId.observeAsState()

    var showDeleteDialog by remember { mutableStateOf<Conversation?>(null) }

    LaunchedEffect(context) {
        MessageRepository.getInstance(context).getAllConversations().observeForever {
            Log.d("MessageCenterScreen", "Conversations: $it")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("消息") }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            })
        }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // 检查用户是否已登录
            if (currentUserId.isNullOrEmpty()) {
                // 用户未登录，显示登录提示
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "请先登录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "登录后查看您的消息", color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "去登录",
                        color = RoseRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate("/login")
                        })
                }
            } else {
                // 用户已登录，显示正常的消息列表内容
                when (connectionState) {
                    ConnectionState.DISCONNECTED, ConnectionState.CONNECTION_ERROR -> {
                        ConnectionStatusBar(connectionState!!)
                    }

                    else -> {}
                }

                when (val state = conversationListState) {
                    is MessageViewModel.ConversationListState.Loading, null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center), color = RoseRed
                        )
                    }

                    is MessageViewModel.ConversationListState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(state.conversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    onPinnedDropdownMenuItemClick = {
//                                        viewModel.togglePin(conversation.conversation.id, !conversation.userSettings.isPinned)
                                    },
                                    onMutedDropdownMenuItemClick = {
//                                        viewModel.toggleMute(conversation.conversation.id, !conversation.userSettings.isMuted)
                                    },
                                    onItemClick = {
                                        navController.navigate("/messages/${conversation.participantId}")
                                    },
                                    onDeleteClick = { showDeleteDialog = conversation })
                            }
                        }
                    }

                    is MessageViewModel.ConversationListState.Error -> {
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
                                onClick = { viewModel.loadConversations() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                            ) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }

        // 删除确认对话框
        showDeleteDialog?.let { conversation ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("删除会话") },
                text = { Text("确定要删除这个会话吗？此操作不可恢复。") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteConversation(conversation.id)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("取消")
                    }
                })
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun ConversationItem(
    conversation: Conversation,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPinnedDropdownMenuItemClick: () -> Unit = {},
    onMutedDropdownMenuItemClick: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val participantId = conversation.participantId
    var participant by remember { mutableStateOf<UserAbstract?>(null) }

    // 获取对方用户信息
    LaunchedEffect(participantId) {
        if (participantId != "server") {
            val profileRepository = ProfileRepository(context)
            val result = profileRepository.getUserAbstract(participantId)
            if (result is Resource.Success) {
                participant = result.data
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick), color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                // 显示不同类型会话的头像
                when (participantId) {
                    "server" -> {
                        // 系统通知使用固定图标
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "系统通知",
                            tint = RoseRed,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .padding(8.dp)
                        )
                    }

                    else -> {
                        // 普通会话使用用户头像
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl(participant?.avatarUrl ?: "test_avatar.png"))
                                .crossfade(true).build(),
                            contentDescription = "头像",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // 显示未读消息数量
                if (conversation.unreadCounts > 0) {
                    Badge(
                        containerColor = RoseRed,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Text(text = conversation.unreadCounts.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 会话名称
                Text(
                    text = when (participantId) {
                        "server" -> "系统通知"
                        else -> participant?.userName ?: "未知用户"
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 最后一条消息内容预览
                val messagePreview = when {
                    conversation.lastMessage == null -> ""
                    conversation.lastMessage?.messageType == "system" -> "[系统通知]"
                    conversation.lastMessage?.messageType == "notification" -> "[互动通知]"
                    conversation.lastMessage?.messageType == "order" -> "[订单消息]"
                    conversation.lastMessage?.messageType == "payment" -> "[支付消息]"
                    conversation.lastMessage?.content?.firstOrNull()?.type == "text" -> 
                        conversation.lastMessage?.content?.firstOrNull()?.payload?.toString() ?: ""
                    conversation.lastMessage?.content?.firstOrNull()?.type == "image" -> "[图片]"
                    conversation.lastMessage?.content?.firstOrNull()?.type == "product" -> "[商品]"
                    else -> "[消息]"
                }
                Text(
                    text = messagePreview,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                // 显示最后一条消息时间
                Text(text = conversation.lastMessage?.createdAt?.let { formatTime(it) } ?: "",
                    color = Color.Gray,
                    fontSize = 12.sp)

                Spacer(modifier = Modifier.height(4.dp))

                // 会话菜单
                Box {
                    IconButton(
                        onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多选项",
                            tint = Color.Gray
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        // 实现"置顶"功能
                        /* 等待后端实现
                        DropdownMenuItem(
                            text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
                            onClick = {
                                showMenu = false
                                onPinnedDropdownMenuItemClick()
                            },
                            leadingIcon = {
                                Icon(
                                    if (conversation.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                    contentDescription = null
                                )
                            })
                        */
                        
                        // 实现"免打扰"功能
                        /* 等待后端实现
                        DropdownMenuItem(
                            text = { Text(if (conversation.isMuted) "取消免打扰" else "设为免打扰") },
                            onClick = {
                                showMenu = false
                                onMutedDropdownMenuItemClick()
                            },
                            leadingIcon = {
                                Icon(
                                    if (conversation.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                    contentDescription = null
                                )
                            })
                        */
                        
                        // 删除会话选项
                        DropdownMenuItem(
                            text = { Text("删除会话") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBar(state: ConnectionState) {
    Surface(
        color = when (state) {
            ConnectionState.CONNECTION_ERROR -> Color(0xFFFFEBEE)
            else -> Color(0xFFFFF3E0)
        }, modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (state) {
                    ConnectionState.CONNECTION_ERROR -> Icons.Default.Error
                    else -> Icons.Default.Warning
                }, contentDescription = null, tint = when (state) {
                    ConnectionState.CONNECTION_ERROR -> Color.Red
                    else -> Color(0xFFFFA000)
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (state) {
                    ConnectionState.DISCONNECTED -> "连接已断开，正在重新连接..."
                    ConnectionState.CONNECTION_ERROR -> "连接失败，请检查网络"
                    else -> "未知状态"
                }, color = Color.DarkGray
            )
        }
    }
}