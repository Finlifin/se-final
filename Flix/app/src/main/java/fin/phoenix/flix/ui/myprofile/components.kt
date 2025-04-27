package fin.phoenix.flix.ui.myprofile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fin.phoenix.flix.data.User
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.colors.WarnRoseRed
import fin.phoenix.flix.util.imageUrl


@Composable
fun ProfileContent(user: User, navController: NavController) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Profile header with user info
        ProfileHeader(user, onEditProfile = { navController.navigate("/my_profile/edit") })

        Spacer(modifier = Modifier.height(24.dp))

        // Wallet section
        WalletSection(
            balance = user.balance, onTopUpClick = { navController.navigate("/profile/top_up") })

        Spacer(modifier = Modifier.height(24.dp))

        // My Products section
        MenuSection(
            title = "我的商品",
            items = listOf(
                MenuItem("我发布的") { navController.navigate("/my_profile/my_products") },
                MenuItem("我卖出的") { navController.navigate("/my_profile/sold_products") },
                MenuItem("我买到的") { navController.navigate("/my_profile/purchased_products") })
        )

        Spacer(modifier = Modifier.height(16.dp))

        // My Transactions section
        MenuSection(
            title = "我的交易", items = listOf(
                MenuItem("我的订单") { navController.navigate("/orders") },
                MenuItem("收藏夹") { navController.navigate("/my_profile/favorites") },
                MenuItem("消息中心") { navController.navigate("/profile/messages") })
        )
    }
}

@Composable
private fun ProfileHeader(user: User, onEditProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        // User avatar
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.avatarUrl?.let { imageUrl(it) }).crossfade(true).build(),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.userName, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "手机号: ${user.phoneNumber}", fontSize = 14.sp, color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "地址: ${user.currentAddress ?: "未设置"}", fontSize = 14.sp, color = Color.Gray
            )
            
            // 添加学校和校区信息
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "学校: ${user.schoolId ?: "未设置"}", fontSize = 14.sp, color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "校区: ${user.campusId ?: "未设置"}", fontSize = 14.sp, color = Color.Gray
            )
        }

        // Edit profile button
        IconButton(onClick = onEditProfile) {
            Icon(
                imageVector = Icons.Default.Edit, contentDescription = "编辑资料", tint = RoseRed
            )
        }
    }
}

@Composable
private fun WalletSection(balance: Int, onTopUpClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "钱包余额", fontSize = 14.sp, color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "¥ $balance", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RoseRed
            )
        }

        Button(
            onClick = onTopUpClick, colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
        ) {
            Text("充值")
        }
    }
}

@Composable
private fun MenuSection(title: String, items: List<MenuItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        items.forEach { item ->
            MenuItemRow(item)
            if (item != items.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title, modifier = Modifier.weight(1f), color = item.fontColor
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}

data class MenuItem(val title: String, val fontColor: Color = Color.Black, val onClick: () -> Unit)
