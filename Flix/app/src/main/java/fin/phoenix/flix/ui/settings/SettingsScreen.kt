package fin.phoenix.flix.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.ui.colors.RoseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = viewModel()
    val isDarkMode by viewModel.darkMode.observeAsState(initial = false)
    val notificationsEnabled by viewModel.notificationsEnabled.observeAsState(initial = true)
    val language by viewModel.language.observeAsState(initial = "简体中文")
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("选择语言") },
            text = {
                Column {
                    listOf("简体中文", "English").forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = lang == language,
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = lang)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        navController.navigate("/login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F8F8)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsGroup(title = "应用设置") {
                    SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = "深色模式",
                        checked = isDarkMode,
                        onCheckedChange = viewModel::setDarkMode
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = "通知提醒",
                        checked = notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled
                    )

                    SettingsClickItem(
                        icon = Icons.Default.Language,
                        title = "语言",
                        subtitle = language,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            item {
                SettingsGroup(title = "账号与安全") {
                    SettingsClickItem(
                        title = "修改密码",
                        onClick = { navController.navigate("/settings/change_password") }
                    )
                    SettingsClickItem(
                        title = "隐私设置",
                        onClick = { navController.navigate("/settings/privacy") }
                    )
                    SettingsClickItem(
                        title = "注销账号",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showLogoutDialog = true }
                    )
                }
            }

            item {
                SettingsGroup(title = "关于") {
                    SettingsClickItem(
                        title = "关于我们",
                        onClick = { navController.navigate("/about") }
                    )
                    SettingsClickItem(
                        title = "隐私政策",
                        onClick = { navController.navigate("/privacy_policy") }
                    )
                    SettingsClickItem(
                        title = "版本",
                        subtitle = "1.0.0",
                        enabled = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, MaterialTheme.shapes.medium)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector? = null,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(text = title)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RoseRed,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    textColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        if (enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}