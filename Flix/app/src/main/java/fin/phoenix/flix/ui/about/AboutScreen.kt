package fin.phoenix.flix.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import fin.phoenix.flix.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于我们") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(vertical = 16.dp)
                )
            }

            item {
                Text(
                    text = "Flix",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Text(
                    text = "版本 1.0.0",
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Flix 是一个致力于为大学生提供二手交易服务的平台。我们的目标是让闲置物品流通起来，让资源得到更好的利用。",
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        
                        Text(
                            text = "我们的团队由一群充满热情的年轻人组成，致力于为用户提供最好的二手交易体验。",
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        
                        Text(
                            text = "如果您有任何问题或建议，欢迎随时联系我们：",
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        
                        Text(
                            text = "support@flix.com",
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}