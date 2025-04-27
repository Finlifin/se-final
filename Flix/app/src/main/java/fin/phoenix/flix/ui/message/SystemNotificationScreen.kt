package fin.phoenix.flix.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import fin.phoenix.flix.data.InteractionPayload
import fin.phoenix.flix.data.MessageContentItem
import fin.phoenix.flix.data.MessageStatus
import fin.phoenix.flix.data.SystemAnnouncementPayload
import fin.phoenix.flix.data.SystemNotificationPayload
import fin.phoenix.flix.ui.colors.RoseRed
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemNotificationScreen(
    navController: NavController, conversationId: String
) {
    val viewModel: MessageViewModel = viewModel()
    val chatState by viewModel.chatState.observeAsState()

    LaunchedEffect(conversationId) {
        val messageType = if(conversationId.startsWith("system_announcement")) {
            "系统公告"
        } else if(conversationId.startsWith("system_notification")) {
            "系统通知"
        } else if(conversationId.startsWith("interaction")) {
            "互动消息"
        } else {
            "通知"
        }
//        viewModel.loadSystemMessages(messageType)
        viewModel.loadChat(conversationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    text = when (conversationId) {
                        "system_notification" -> "系统通知"
                        "system_announcement" -> "系统公告"
                        "interaction" -> "互动消息"
                        else -> "通知"
                    }
                )
            }, navigationIcon = {
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
            when (val state = chatState) {
                is MessageViewModel.ChatState.Loading, null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), color = RoseRed
                    )
                }

                is MessageViewModel.ChatState.Success -> {
                    if (state.messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无消息", color = Color.Gray, fontSize = 16.sp
                            )
                        }
                    } else {
                        val groupedMessages =
                            state.messages.filter { it.messageType == conversationId }
                                .sortedByDescending { it.insertedAt }.groupBy {
                                    when {
                                        isToday(it.insertedAt) -> "今天"
                                        isYesterday(it.insertedAt) -> "昨天"
                                        isThisWeek(it.insertedAt) -> "本周"
                                        isThisMonth(it.insertedAt) -> "本月"
                                        else -> "更早"
                                    }
                                }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupedMessages.forEach { (group, messages) ->
                                item {
                                    Text(
                                        text = group,
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                items(messages) { message ->
                                    SystemMessageItem(
                                        message = message.content.firstOrNull(),
                                        messageType = conversationId,
                                        isUnread = message.status == MessageStatus.UNREAD,
                                        timestamp = message.insertedAt,
                                        onItemClick = { deepLink ->
                                            deepLink?.let { navController.navigate(it) }
                                            viewModel.markSystemMessageRead(
                                                conversationId,
                                                message.id
                                            )
                                        })
                                }
                            }
                        }
                    }

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
                            onClick = { viewModel.loadSystemMessages(conversationId) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemMessageItem(
    message: MessageContentItem?,
    messageType: String,
    isUnread: Boolean = true,
    timestamp: Date? = null,
    onItemClick: (String?) -> Unit
) {
    if (message == null) return

    val gson = Gson()
    val payload = when (messageType) {
        "system_notification" -> gson.fromJson(
            gson.toJson(message.payload),
            SystemNotificationPayload::class.java
        )

        "system_announcement" -> gson.fromJson(
            gson.toJson(message.payload),
            SystemAnnouncementPayload::class.java
        )

        "interaction" -> gson.fromJson(gson.toJson(message.payload), InteractionPayload::class.java)
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isUnread) Color(0xFFFAFAFA) else Color.White,
        onClick = {
            when (payload) {
                is SystemNotificationPayload -> onItemClick(payload.deepLink)
                is SystemAnnouncementPayload -> onItemClick(payload.deepLink)
                is InteractionPayload -> onItemClick(payload.deepLink)
                else -> onItemClick(null)
            }
        }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = when (messageType) {
                        "system_notification" -> Icons.Default.Notifications
                        "system_announcement" -> Icons.Default.Campaign
                        "interaction" -> Icons.AutoMirrored.Filled.Chat
                        else -> Icons.Default.Info
                    }, contentDescription = null, tint = RoseRed, modifier = Modifier.size(24.dp)
                )

                Column {
                    when (payload) {
                        is SystemNotificationPayload -> {
                            Text(
                                text = payload.title,
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (isUnread) Color.Black else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = payload.text,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
                        }

                        is SystemAnnouncementPayload -> {
                            Text(
                                text = payload.title,
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (isUnread) Color.Black else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = payload.text,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
                        }

                        is InteractionPayload -> {
                            Text(
                                text = payload.title,
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (isUnread) Color.Black else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = payload.text,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatRelativeTime(timestamp ?: Date()),
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                if (isUnread) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(RoseRed, CircleShape)
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}天前"
        hours > 0 -> "${hours}小时前"
        minutes > 0 -> "${minutes}分钟前"
        else -> "刚刚"
    }
}

private fun isToday(date: Date): Boolean {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    calendar.time = date
    val messageDay = calendar.get(Calendar.DAY_OF_YEAR)
    return today == messageDay && calendar.get(Calendar.YEAR) == Calendar.getInstance()
        .get(Calendar.YEAR)
}

private fun isYesterday(date: Date): Boolean {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = calendar.get(Calendar.DAY_OF_YEAR)
    calendar.time = date
    val messageDay = calendar.get(Calendar.DAY_OF_YEAR)
    return yesterday == messageDay && calendar.get(Calendar.YEAR) == Calendar.getInstance()
        .get(Calendar.YEAR)
}

private fun isThisWeek(date: Date): Boolean {
    val calendar = Calendar.getInstance()
    val thisWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    calendar.time = date
    val messageWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    return thisWeek == messageWeek && calendar.get(Calendar.YEAR) == Calendar.getInstance()
        .get(Calendar.YEAR)
}

private fun isThisMonth(date: Date): Boolean {
    val calendar = Calendar.getInstance()
    val thisMonth = calendar.get(Calendar.MONTH)
    calendar.time = date
    val messageMonth = calendar.get(Calendar.MONTH)
    return thisMonth == messageMonth && calendar.get(Calendar.YEAR) == Calendar.getInstance()
        .get(Calendar.YEAR)
}