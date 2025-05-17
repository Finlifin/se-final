package fin.phoenix.flix.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    val privacyPolicySections = listOf(
        PrivacySection(
            "信息收集",
            "我们收集的信息包括但不限于：\n" +
            "• 账户信息（用户名、密码、手机号）\n" +
            "• 个人资料（头像、地址）\n" +
            "• 交易信息（发布的商品、购买记录）\n" +
            "• 设备信息（设备ID、IP地址）"
        ),
        PrivacySection(
            "信息使用",
            "我们使用收集的信息用于：\n" +
            "• 提供、维护和改进我们的服务\n" +
            "• 处理您的交易\n" +
            "• 发送服务相关通知\n" +
            "• 防止欺诈和提升安全性"
        ),
        PrivacySection(
            "信息共享",
            "我们不会出售您的个人信息。仅在以下情况下可能共享信息：\n" +
            "• 经您同意\n" +
            "• 法律要求\n" +
            "• 服务提供商需要"
        ),
        PrivacySection(
            "信息安全",
            "我们采取多种安全措施保护您的信息：\n" +
            "• 数据加密传输\n" +
            "• 访问控制\n" +
            "• 安全存储"
        ),
        PrivacySection(
            "您的权利",
            "您对您的个人信息享有以下权利：\n" +
            "• 访问和获取副本\n" +
            "• 更正或更新\n" +
            "• 删除\n" +
            "• 撤回同意"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐私政策") },
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
                Text(
                    text = "最后更新：2024年3月1日",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(privacyPolicySections) { section ->
                PrivacyPolicySection(section)
            }
            
            item {
                Text(
                    text = "如果您对我们的隐私政策有任何疑问，请联系：privacy@flix.com",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyPolicySection(section: PrivacySection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = section.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = section.content,
                lineHeight = 24.sp
            )
        }
    }
}

private data class PrivacySection(
    val title: String,
    val content: String
)