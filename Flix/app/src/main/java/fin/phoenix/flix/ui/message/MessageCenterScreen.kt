package fin.phoenix.flix.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import fin.phoenix.flix.data.ConversationDetail
import fin.phoenix.flix.data.ConversationTypes
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.formatDate
import fin.phoenix.flix.util.imageUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(navController: NavController) {
    val viewModel: MessageViewModel = viewModel()
    val conversationListState by viewModel.conversationListState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val unreadCounts by viewModel.unreadCounts.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<ConversationDetail?>(null) }

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
            when (connectionState) {
                ConnectionState.DISCONNECTED, ConnectionState.CONNECTION_ERROR -> {
                    ConnectionStatusBar(connectionState)
                }

                else -> {}
            }

            when (val state = conversationListState) {
                is MessageViewModel.ConversationListState.Loading -> {
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
                                    viewModel.togglePin(conversation.conversation.id, !conversation.userSettings.isPinned)
                                },
                                onMuttedDropdownMenuItemClick = {
                                    viewModel.toggleMute(conversation.conversation.id, !conversation.userSettings.isMuted)
                                },
                                onItemClick = {
                                    when (conversation.conversation.type) {
                                        ConversationTypes.PRIVATE -> {
                                            navController.navigate("/messages/${conversation.conversation.id}")
                                        }

                                        ConversationTypes.SYSTEM_NOTIFICATION -> {
                                            navController.navigate("/notifications/system")
                                        }

                                        ConversationTypes.SYSTEM_ANNOUNCEMENT -> {
                                            navController.navigate("/notifications/announcement")
                                        }

                                        ConversationTypes.INTERACTION -> {
                                            navController.navigate("/notifications/interaction")
                                        }
                                    }
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

        // 删除确认对话框
        showDeleteDialog?.let { conversation ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("删除会话") },
                text = { Text("确定要删除这个会话吗？此操作不可恢复。") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteConversation(conversation.conversation.id)
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

@Composable
fun ConversationItem(
    conversation: ConversationDetail,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPinnedDropdownMenuItemClick: () -> Unit = {},
    onMuttedDropdownMenuItemClick: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentUser = UserManager.getInstance(context).currentUser

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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        // TODO
                        .data(imageUrl("")).crossfade(true)
                        .build(),
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )

                if (conversation.userSettings.unreadCount > 0) {
                    Badge(
                        containerColor = RoseRed,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Text(text = conversation.userSettings.unreadCount.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (conversation.conversation.type) {
                        ConversationTypes.PRIVATE -> conversation.conversation.counterPartyId(currentUser.value!!.uid)
                            ?: "未知用户"

                        ConversationTypes.SYSTEM_NOTIFICATION -> "系统通知"
                        ConversationTypes.SYSTEM_ANNOUNCEMENT -> "系统公告"
                        ConversationTypes.INTERACTION -> "互动消息"
                        else -> "未知会话"
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = conversation.conversation.lastMessageContent ?: "",
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
                Text(
                    text = formatDate(conversation.conversation.lastMessageTimestamp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

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
                        DropdownMenuItem(
                            text = { Text(if (conversation.userSettings.isPinned) "取消置顶" else "置顶") },
                            onClick = {
                                showMenu = false
                                onPinnedDropdownMenuItemClick()
                            },
                            leadingIcon = {
                                Icon(
                                    if (conversation.userSettings.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                    contentDescription = null
                                )
                            })
                        DropdownMenuItem(
                            text = { Text(if (conversation.userSettings.isMuted) "取消免打扰" else "设为免打扰") },
                            onClick = {
                                showMenu = false
                                onMuttedDropdownMenuItemClick()
                            },
                            leadingIcon = {
                                Icon(
                                    if (conversation.userSettings.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                    contentDescription = null
                                )
                            })
                        DropdownMenuItem(text = { Text("删除会话") }, onClick = {
                            showMenu = false
                            onDeleteClick()
                        }, leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        })
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