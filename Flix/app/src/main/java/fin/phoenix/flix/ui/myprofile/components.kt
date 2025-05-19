package fin.phoenix.flix.ui.myprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fin.phoenix.flix.data.Campus
import fin.phoenix.flix.data.School
import fin.phoenix.flix.data.User
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.imageUrl

@Composable
fun ProfileContent(user: User, navController: NavController, viewModel: MyProfileViewModel) {
    LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Profile header with user info
        ProfileHeader(
            user,
            onEditProfile = { navController.navigate("/my_profile/edit") },
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Wallet section
        WalletSection(
            balance = user.balance,
            onTopUpClick = { navController.navigate("/my_profile/recharge") })

        Spacer(modifier = Modifier.height(24.dp))

        // My Transactions section
        MenuSection(
            title = "我的交易",
            items = listOf(
                MenuItem("我的订单") { navController.navigate("/orders") },
                MenuItem("收藏夹") { navController.navigate("/my_profile/favorites") },
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        // My Products section
        MenuSection(
            title = "我的商品",
            items = listOf(
                MenuItem("我发布的") { navController.navigate("/my_profile/my_products") },
                MenuItem("我卖出的") { navController.navigate("/my_profile/sold_products") },
                MenuItem("我买到的") { navController.navigate("/my_profile/purchased_products") })
        )

    }
}

@Composable
fun ProfileHeader(user: User, onEditProfile: () -> Unit, viewModel: MyProfileViewModel) {
    // 观察学校和校区名称状态
    val schoolNameState by viewModel.schoolNameState.observeAsState(initial = Resource.Loading)
    val campusNameState by viewModel.campusNameState.observeAsState(initial = Resource.Loading)

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
                text = "地址: ${user.currentAddress ?: "未设置"}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            // 显示学校信息
            Spacer(modifier = Modifier.height(4.dp))
            when (schoolNameState) {
                is Resource.Loading -> {
                    Text(
                        text = "学校: 加载中...", fontSize = 14.sp, color = Color.Gray
                    )
                }

                is Resource.Error -> {
                    Text(
                        text = "学校: 获取失败", fontSize = 14.sp, color = Color.Gray
                    )
                }

                is Resource.Success -> {
                    val schoolName = (schoolNameState as Resource.Success<String>).data
                    Text(
                        text = "学校: $schoolName", fontSize = 14.sp, color = Color.Gray
                    )
                }
            }

            // 显示校区信息
            Spacer(modifier = Modifier.height(4.dp))
            when (campusNameState) {
                is Resource.Loading -> {
                    Text(
                        text = "校区: 加载中...", fontSize = 14.sp, color = Color.Gray
                    )
                }

                is Resource.Error -> {
                    Text(
                        text = "校区: 获取失败", fontSize = 14.sp, color = Color.Gray
                    )
                }

                is Resource.Success -> {
                    val campusName = (campusNameState as Resource.Success<String>).data
                    Text(
                        text = "校区: $campusName", fontSize = 14.sp, color = Color.Gray
                    )
                }
            }
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

// 表单区域组件
@Composable
fun FormSection(
    title: String, icon: ImageVector, content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardColors(Color.White, Color.Black, Color.White, Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RoseRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                thickness = 1.dp,
                color = Color(0xFFEEEEEE)
            )

            content()
        }
    }
}

// 增强型表单字段组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = label, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = RoseRed)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
            )
        )
    }
}

// 地址管理组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAddressesManager(
    addresses: List<String>,
    currentAddress: String,
    onCurrentAddressChanged: (String) -> Unit,
    onAddressAdded: (String) -> Unit,
    onAddressRemoved: (String) -> Unit
) {
    var newAddress by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column {
        // Current address selection
        Text(
            text = "当前地址",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = currentAddress.ifEmpty { "请选择地址" },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                leadingIcon = {
                    Icon(Icons.Default.Home, contentDescription = null, tint = RoseRed)
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                )
            )

            if (addresses.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded, onDismissRequest = { expanded = false }) {
                    addresses.forEach { address ->
                        DropdownMenuItem(text = { Text(address) }, onClick = {
                            onCurrentAddressChanged(address)
                            expanded = false
                        }, leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (address == currentAddress) RoseRed else Color.Gray
                            )
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Address list
        Text(
            text = "我的地址列表",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (addresses.isEmpty()) {
            Text(
                text = "暂无保存的地址",
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    addresses.forEach { address ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (address == currentAddress) RoseRed else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = address, modifier = Modifier.weight(1f)
                                )
                            }

                            IconButton(
                                onClick = { onAddressRemoved(address) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除地址",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (address != addresses.last()) {
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add new address
        Text(
            text = "添加新地址",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = newAddress,
                onValueChange = { newAddress = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入新的地址") },
                leadingIcon = {
                    Icon(Icons.Default.AddLocation, contentDescription = null, tint = RoseRed)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    onAddressAdded(newAddress.trim())
                    newAddress = ""
                }, enabled = newAddress.trim().isNotBlank(), colors = ButtonDefaults.buttonColors(
                    containerColor = RoseRed, disabledContainerColor = Color.LightGray
                ), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("添加")
            }
        }
    }
}

// 学校选择底部表单
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectionBottomSheet(
    isVisible: Boolean,
    schoolSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    schoolsState: Resource<List<School>>,
    onSchoolSelected: (id: String, name: String) -> Unit,
    onAddSchoolClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss, sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 顶部标题
                Text(
                    text = "选择学校",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 搜索输入框
                OutlinedTextField(
                    value = schoolSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("搜索学校") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search, contentDescription = "搜索", tint = RoseRed
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 学校列表
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = schoolsState) {
                        is Resource.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = RoseRed)
                            }
                        }

                        is Resource.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "加载失败",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.message ?: "未知错误",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { /* 重新加载学校列表 - 这应该由外部处理 */ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoseRed
                                    )
                                ) {
                                    Text("重试")
                                }
                            }
                        }

                        is Resource.Success -> {
                            val schools = state.data
                            val filteredSchools = if (schoolSearchQuery.isBlank()) {
                                schools
                            } else {
                                schools.filter {
                                    it.name.contains(
                                        schoolSearchQuery, ignoreCase = true
                                    )
                                }
                            }

                            if (filteredSchools.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "没有找到匹配的学校", color = Color.Gray
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 添加"不设置"选项
                                    item {
                                        SchoolItem(
                                            name = "不设置校区",
                                            icon = Icons.Default.Close,
                                            iconTint = Color.Gray,
                                            onClick = { onSchoolSelected("", "未设置") })
                                    }

                                    // 学校列表
                                    items(filteredSchools) { school ->
                                        SchoolItem(
                                            name = school.name,
                                            subText = school.city,
                                            icon = Icons.Default.School,
                                            iconTint = RoseRed,
                                            onClick = { onSchoolSelected(school.id, school.name) })
                                    }
                                }
                            }
                        }
                    }
                }

                // 底部添加按钮
                Button(
                    onClick = onAddSchoolClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加新学校")
                }
            }
        }
    }
}

// 校区选择底部表单
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSelectionBottomSheet(
    isVisible: Boolean,
    schoolName: String,
    campusSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    campusesState: Resource<List<Campus>>,
    onCampusSelected: (id: String, name: String) -> Unit,
    onAddCampusClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss, sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 顶部标题和学校名称
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "选择校区", fontWeight = FontWeight.Bold, fontSize = 18.sp
                    )

                    Text(
                        text = "当前学校：$schoolName", color = Color.Gray, fontSize = 14.sp
                    )
                }

                // 搜索输入框
                OutlinedTextField(
                    value = campusSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("搜索校区") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search, contentDescription = "搜索", tint = RoseRed
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 校区列表
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = campusesState) {
                        is Resource.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = RoseRed)
                            }
                        }

                        is Resource.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "加载失败",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = state.message ?: "未知错误",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { /* 重新加载校区列表 - 这应该由外部处理 */ },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoseRed
                                    )
                                ) {
                                    Text("重试")
                                }
                            }
                        }

                        is Resource.Success -> {
                            val campuses = state.data
                            val filteredCampuses = if (campusSearchQuery.isBlank()) {
                                campuses
                            } else {
                                campuses.filter {
                                    it.name.contains(
                                        campusSearchQuery, ignoreCase = true
                                    )
                                }
                            }

                            if (filteredCampuses.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (campuses.isEmpty()) "该学校暂无校区信息"
                                        else "没有找到匹配的校区", color = Color.Gray
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 添加"不设置"选项
                                    item {
                                        CampusItem(
                                            name = "不设置校区",
                                            icon = Icons.Default.Close,
                                            iconTint = Color.Gray,
                                            onClick = { onCampusSelected("", "未设置") })
                                    }

                                    // 校区列表
                                    items(filteredCampuses) { campus ->
                                        CampusItem(
                                            name = campus.name,
                                            address = campus.address,
                                            icon = Icons.Default.LocationOn,
                                            iconTint = RoseRed,
                                            onClick = { onCampusSelected(campus.id, campus.name) })
                                    }
                                }
                            }
                        }
                    }
                }

                // 底部添加按钮
                Button(
                    onClick = onAddCampusClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加新校区")
                }
            }
        }
    }
}

// 添加新学校对话框
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSchoolDialog(
    isVisible: Boolean,
    schoolName: String,
    onSchoolNameChange: (String) -> Unit,
    schoolCode: String,
    onSchoolCodeChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(onDismissRequest = onDismiss, title = {
            Text("添加新学校", fontWeight = FontWeight.Bold)
        }, text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = onSchoolNameChange,
                    label = { Text("学校名称") },
                    placeholder = { Text("请输入学校名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                    )
                )

                // 学校代码相关已弃用，以下为遗留代码，已注释
//                    OutlinedTextField(
//                        value = schoolCode,
//                        onValueChange = onSchoolCodeChange,
//                        label = { Text("学校代码") },
//                        placeholder = { Text("请输入学校代码") },
//                        singleLine = true,
//                        modifier = Modifier.fillMaxWidth(),
//                        colors = OutlinedTextFieldDefaults.colors(
//                            focusedBorderColor = RoseRed,
//                            unfocusedBorderColor = Color.Gray
//                        )
//                    )
//
//                    Text(
//                        text = "学校代码是学校的唯一标识，通常为学校名称的拼音首字母，例如：PKU (北京大学)",
//                        fontSize = 12.sp,
//                        color = Color.Gray
//                    )
            }
        }, confirmButton = {
            Button(
                onClick = onAddClick,
                // enabled = schoolName.isNotBlank() && schoolCode.isNotBlank(), // 学校代码已弃用
                enabled = schoolName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
            ) {
                Text("添加")
            }
        }, dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        })
    }
}

// 添加新校区对话框
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCampusDialog(
    isVisible: Boolean,
    schoolName: String,
    campusName: String,
    onCampusNameChange: (String) -> Unit,
    campusAddress: String,
    onCampusAddressChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(onDismissRequest = onDismiss, title = {
            Text("添加新校区", fontWeight = FontWeight.Bold)
        }, text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "所属学校：$schoolName",
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = campusName,
                    onValueChange = onCampusNameChange,
                    label = { Text("校区名称") },
                    placeholder = { Text("请输入校区名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = campusAddress,
                    onValueChange = onCampusAddressChange,
                    label = { Text("校区地址") },
                    placeholder = { Text("请输入校区地址") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                    )
                )
            }
        }, confirmButton = {
            Button(
                onClick = onAddClick,
                enabled = campusName.isNotBlank() && campusAddress.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
            ) {
                Text("添加")
            }
        }, dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        })
    }
}

// 学校列表项组件
@Composable
fun SchoolItem(
    name: String, subText: String? = null, icon: ImageVector, iconTint: Color, onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name)
                if (subText != null) {
                    Text(
                        text = subText, fontSize = 12.sp, color = Color.Gray
                    )
                }
            }
        }
    }
}

// 校区列表项组件
@Composable
fun CampusItem(
    name: String, address: String? = null, icon: ImageVector, iconTint: Color, onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name)
                if (address != null) {
                    Text(
                        text = address, fontSize = 12.sp, color = Color.Gray
                    )
                }
            }
        }
    }
}
