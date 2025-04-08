package fin.phoenix.flix.ui.myprofile

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.components.ErrorMessage
import fin.phoenix.flix.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileContent(navController: NavController, userId: String) {
    val myProfileViewModel: MyProfileViewModel = viewModel()

    // Observe profile state
    val profileState by myProfileViewModel.userProfileState.observeAsState(initial = Resource.Loading)
    val ctx = LocalContext.current

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
