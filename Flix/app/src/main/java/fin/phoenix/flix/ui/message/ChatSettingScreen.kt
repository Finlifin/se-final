package fin.phoenix.flix.ui.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.ui.colors.RoseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingScreen(
    navController: NavController,
    conversationId: String
) {
    val viewModel: MessageViewModel = viewModel()
    var showClearDialog by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("聊天设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 聊天记录设置
            SettingSection(title = "聊天记录") {
                SettingItem(
                    icon = Icons.Default.Delete,
                    title = "清空聊天记录",
                    onClick = { showClearDialog = true }
                )
            }

            HorizontalDivider()

            // 通知设置
            SettingSection(title = "通知设置") {
                SettingItemWithSwitch(
                    icon = Icons.Default.Notifications,
                    title = "消息通知",
                    checked = notificationEnabled,
                    onCheckedChange = { 
                        notificationEnabled = it
                        viewModel.toggleMute(conversationId, !it)
                    }
                )
            }

            HorizontalDivider()

            // 隐私与安全
            SettingSection(title = "隐私与安全") {
                SettingItem(
                    icon = Icons.Default.Report,
                    title = "举报",
                    onClick = { showReportDialog = true }
                )
            }
        }

        // 清空聊天记录的确认对话框
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("清空聊天记录") },
                text = { Text("确定要清空此聊天的所有消息吗？此操作无法恢复。") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearConversation(conversationId)
                            showClearDialog = false
                            // 返回到聊天界面
                            navController.navigateUp()
                        }
                    ) {
                        Text("清空")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 举报对话框
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("举报用户") },
                text = { Text("如果您遇到不良行为或违规内容，可以向我们提交举报。") },
                confirmButton = {
                    Button(
                        onClick = {
                            // 实现举报功能
                            showReportDialog = false
                        }
                    ) {
                        Text("提交举报")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = RoseRed,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingItemWithSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { 
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}