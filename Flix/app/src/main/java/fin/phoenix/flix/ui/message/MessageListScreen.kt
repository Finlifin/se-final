//package fin.phoenix.flix.ui.message
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Tab
//import androidx.compose.material3.TabRow
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import fin.phoenix.flix.data.MessagePreview
//import fin.phoenix.flix.data.MessageTypes
//import fin.phoenix.flix.ui.colors.RoseRed
//import fin.phoenix.flix.ui.message.components.ConversationItem
//import fin.phoenix.flix.ui.message.components.EmptyConversations
//import fin.phoenix.flix.util.Resource
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MessageListScreen(navController: NavController) {
//    val context = LocalContext.current
//    val viewModel: MessageViewModel = viewModel(
//        factory = MessageViewModelFactory(context)
//    )
//
//    // 加载会话列表
//    LaunchedEffect(Unit) {
//        viewModel.loadConversations()
//    }
//
//    // 会话列表状态
//    val conversationsState by viewModel.conversationsState.collectAsState()
//
//    // 当前选中的消息类型
//    val selectedMessageType by viewModel.selectedMessageType.collectAsState()
//
//    // 连接状态
//    val connectionState by viewModel.connectionState.collectAsState()
//
//    // 消息类型选项卡
//    val tabs = listOf(
//        TabItem("私信", MessageTypes.PRIVATE_MESSAGE),
//        TabItem("互动", MessageTypes.INTERACTION),
//        TabItem("系统通知", MessageTypes.SYSTEM_NOTIFICATION),
//        TabItem("系统公告", MessageTypes.SYSTEM_ANNOUNCEMENT)
//    )
//
//    var selectedTabIndex by remember {
//        mutableIntStateOf(tabs.indexOfFirst { it.messageType == selectedMessageType })
//    }
//
//    if (selectedTabIndex == -1) selectedTabIndex = 0
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text("消息")
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.White
//                )
//            )
//        }
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            // 显示连接状态
//            if (connectionState is MessageViewModel.ConnectionState.Connecting ||
//                connectionState is MessageViewModel.ConnectionState.Failed) {
//                val statusMessage = when (connectionState) {
//                    is MessageViewModel.ConnectionState.Connecting -> "正在连接服务器..."
//                    is MessageViewModel.ConnectionState.Failed ->
//                        "连接失败: ${(connectionState as MessageViewModel.ConnectionState.Failed).reason}"
//                    else -> ""
//                }
//
//                if (statusMessage.isNotEmpty()) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(if (connectionState is MessageViewModel.ConnectionState.Failed)
//                                Color(0xFFFF9800) else Color(0xFF42A5F5))
//                            .padding(8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = statusMessage,
//                            color = Color.White,
//                            modifier = Modifier.weight(1f)
//                        )
//
//                        if (connectionState is MessageViewModel.ConnectionState.Failed) {
//                            Text(
//                                text = "重试",
//                                color = Color.White,
//                                modifier = Modifier
//                                    .padding(start = 8.dp)
//                                    .clickable {
//                                        viewModel.reconnect()
//                                    }
//                            )
//                        }
//                    }
//                }
//            }
//
//            // 消息类型选项卡
//            TabRow(
//                selectedTabIndex = selectedTabIndex,
//                containerColor = Color.White,
//                contentColor = RoseRed,
//                indicator = { tabPositions ->
//                    if (selectedTabIndex >= 0 && selectedTabIndex < tabPositions.size) {
//                        Box(
//                            Modifier
//                                .padding(horizontal = 24.dp)
//                                .height(3.dp)
//                                .background(RoseRed)
////                                .align(Alignment.BottomCenter)
//                        )
//                    }
//                }
//            ) {
//                tabs.forEachIndexed { index, tab ->
//                    Tab(
//                        selected = selectedTabIndex == index,
//                        onClick = {
//                            selectedTabIndex = index
//                            viewModel.setMessageType(tab.messageType)
//                        },
//                        text = {
//                            Text(
//                                text = tab.title,
//                                color = if (selectedTabIndex == index) RoseRed else Color.Gray
//                            )
//                        }
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // 消息列表
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f)
//            ) {
//                when (val state = conversationsState) {
//                    is Resource.Loading -> {
//                        // 加载状态
//                        Box(
//                            modifier = Modifier.fillMaxSize(),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            CircularProgressIndicator(color = RoseRed)
//                        }
//                    }
//
//                    is Resource.Error -> {
//                        // 错误状态
//                        Box(
//                            modifier = Modifier.fillMaxSize(),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = state.message ?: "加载失败",
//                                color = Color.Gray,
//                                textAlign = TextAlign.Center,
//                                modifier = Modifier.padding(16.dp)
//                            )
//                        }
//                    }
//
//                    is Resource.Success -> {
//                        val conversations = state.data.filter {
//                            it.messageType == selectedMessageType
//                        }
//
//                        if (conversations.isEmpty()) {
//                            // 空列表状态
//                            EmptyConversations()
//                        } else {
//                            // 会话列表
//                            LazyColumn {
//                                items(conversations, key = { it.id }) { conversation ->
//                                    ConversationItem(
//                                        preview = conversation,
//                                        onClick = {
//                                            if (conversation.messageType == MessageTypes.PRIVATE_MESSAGE) {
//                                                navController.navigate("messages/${conversation.conversationId}")
//                                            } else if (conversation.messageType == MessageTypes.SYSTEM_ANNOUNCEMENT) {
//                                                // 导航到公告详情
//                                                // navController.navigate("${Routes.ANNOUNCEMENT}/${conversation.id}")
//                                            }
//                                        }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
