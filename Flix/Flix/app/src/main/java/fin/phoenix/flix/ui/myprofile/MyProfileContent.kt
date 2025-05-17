package fin.phoenix.flix.ui.myprofile

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.components.ErrorMessage
import fin.phoenix.flix.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileContent(navController: NavController, userId: String) {
    val myProfileViewModel: MyProfileViewModel = viewModel()

    // 获取UserManager实例和当前用户ID
    val ctx = LocalContext.current
    val userManager = UserManager.getInstance(ctx)
    val currentUserId by userManager.currentUserId.observeAsState()

    // Observe profile state
    val profileState by myProfileViewModel.userProfileState.observeAsState(initial = Resource.Loading)

    // 检查用户是否已登录
    if (currentUserId.isNullOrEmpty()) {
        // 用户未登录，显示登录提示
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("个人中心") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8F8F8))
            ) {
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
                        text = "登录后查看您的个人信息",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "去登录",
                        color = RoseRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            navController.navigate("/login") 
                        }
                    )
                }
            }
        }
    } else {
        // 用户已登录，显示正常的个人中心内容
        // Load user data only when not already loaded or on error
        LaunchedEffect(userId) {
            if (profileState is Resource.Loading || profileState is Resource.Error) {
                myProfileViewModel.getUserProfile(userId)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("个人中心") }, actions = {
                    IconButton(onClick = {
                        navController.navigate("/settings")
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
                )
            }) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8F8F8))
            ) {
                when (val state = profileState) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center), color = RoseRed
                        )
                    }

                    is Resource.Error -> {
                        ErrorMessage(
                            error = state.message,
                            onRetry = { myProfileViewModel.getUserProfile(userId) })
                    }

                    is Resource.Success -> {
                        val user = state.data
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8F8F8)),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                ProfileContent(user, navController)
                            }
                        }
                    }
                }
            }
        }
    }
}
