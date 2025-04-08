package fin.phoenix.flix.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.message.components.ConversationItem
import fin.phoenix.flix.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: MessageViewModel = viewModel(
        factory = MessageViewModelFactory(context)
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // 监听对话列表状态
    val conversationsState by viewModel.conversationsState.collectAsState()

    // 定义消息类型标签页
    val tabs = listOf(
        TabItem("私信", MessageTypes.PRIVATE_MESSAGE),
        TabItem("系统通知", MessageTypes.SYSTEM_NOTIFICATION),
        TabItem("公告", MessageTypes.SYSTEM_ANNOUNCEMENT),
        TabItem("互动", MessageTypes.INTERACTION)
    )

    LaunchedEffect(Unit) {
        viewModel.setMessageType(tabs[1].messageType)
        viewModel.setMessageType(tabs[0].messageType)
    }

    // 当选择标签页变化时更新消息类型筛选
    LaunchedEffect(selectedTabIndex) {
        viewModel.setMessageType(tabs[selectedTabIndex].messageType)
    }

    Scaffold { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
        ) {
            // 标签页
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = Color.White,
                contentColor = RoseRed,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            // 对话列表
            Box(modifier = Modifier.weight(1f)) {
                when (val state = conversationsState) {
                    is Resource.Loading -> {
                        // 加载状态
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = RoseRed,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    is Resource.Error -> {
                        // 错误状态
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "加载失败",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message ?: "未知错误",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    is Resource.Success -> {
                        val conversations = state.data

                        if (conversations.isEmpty()) {
                            // 空数据状态
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无消息",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // 数据列表
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(conversations) { conversation ->
                                    ConversationItem(
                                        conversation = conversation,
                                        onClick = {
                                            // 导航到聊天界面
                                            if (conversation.messageType == MessageTypes.PRIVATE_MESSAGE) {
                                                navController.navigate("/messages/${conversation.conversationId}")
                                            } else {
                                                // 系统消息、公告等其他类型消息的处理
                                                // 根据类型跳转不同页面或显示详情
                                                if (conversation.messageType == MessageTypes.SYSTEM_ANNOUNCEMENT) {
                                                    navController.navigate("/announcements/${conversation.conversationId}")
                                                } else {
                                                    navController.navigate("/notifications/${conversation.conversationId}")
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 标签页项数据类
 */
data class TabItem(val title: String, val messageType: String)