package fin.phoenix.flix.ui.myprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fin.phoenix.flix.data.User
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.imageUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(navController: NavController, userId: String) {
    val viewModel: ProfileEditViewModel = viewModel()
    val userState by viewModel.userState.observeAsState(initial = Resource.Loading)
    val updateState by viewModel.updateState.observeAsState(initial = null)
    
    var userName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addresses by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentAddress by remember { mutableStateOf("") }
    var schoolId by remember { mutableStateOf("") }
    var campusId by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // 预定义的学校和校区列表 (实际应用中应该从API获取)
    val schoolOptions = listOf("未设置", "复旦大学", "上海交通大学", "同济大学", "华东师范大学")
    val campusOptions = listOf("未设置", "邯郸校区", "枫林校区", "江湾校区", "张江校区")

    // Show error snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load user data when screen is first displayed
    LaunchedEffect(userId) {
        viewModel.getUserProfile(userId)
    }

    // Initialize fields when user data is loaded
    LaunchedEffect(userState) {
        if (userState is Resource.Success<User>) {
            val user = (userState as Resource.Success<User>).data
            userName = user.userName
            phone = user.phoneNumber
            addresses = user.addresses
            currentAddress = user.currentAddress ?: ""
            schoolId = user.schoolId ?: "未设置"
            campusId = user.campusId ?: "未设置"
        }
    }

    // Handle update state changes
    LaunchedEffect(updateState) {
        when (updateState) {
            is Resource.Success<*> -> {
                navController.popBackStack()
            }
            is Resource.Error -> {
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar("保存失败: ${(updateState as Resource.Error).message}")
                }
            }
            else -> {}
        }
    }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            avatarUri = it
            // TODO: Upload avatar image to server
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑个人资料") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            isLoading = true
                            if (userState is Resource.Success<User>) {
                                val currentUser = (userState as Resource.Success<User>).data
                                val updatedUser = currentUser.copy(
                                    userName = userName,
                                    addresses = addresses,
                                    currentAddress = currentAddress.takeIf { it.isNotBlank() && it != "未设置" },
                                    schoolId = schoolId.takeIf { it.isNotBlank() && it != "未设置" },
                                    campusId = campusId.takeIf { it.isNotBlank() && it != "未设置" }
                                )
                                viewModel.updateProfile(updatedUser) { success ->
                                    if (success) {
                                        navController.popBackStack()
                                    } else {
                                        isLoading = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("保存失败，请重试")
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && userName.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存", color = RoseRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF5F5F5))
        ) {
            when (userState) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoseRed)
                    }
                }
                
                is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "加载失败：${(userState as Resource.Error).message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                is Resource.Success -> {
                    val user = (userState as Resource.Success<User>).data
                    
                    // Avatar section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(avatarUri ?: imageUrl(user.avatarUrl ?: ""))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "头像",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Edit icon overlay
                                IconButton(
                                    onClick = { imagePicker.launch("image/*") },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(RoseRed)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "更换头像",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("点击更换头像", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Form sections
                    FormSection(
                        title = "基本信息",
                        icon = Icons.Default.Person
                    ) {
                        EnhancedFormField(
                            label = "用户名",
                            value = userName,
                            onValueChange = { userName = it },
                            icon = Icons.Default.EditNote
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        EnhancedFormField(
                            label = "手机号",
                            value = phone,
                            onValueChange = { phone = it },
                            enabled = false,
                            icon = Icons.Default.Phone
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // School & Campus Section
                    FormSection(
                        title = "学校信息",
                        icon = Icons.Default.School
                    ) {
                        // School Dropdown
                        var schoolExpanded by remember { mutableStateOf(false) }
                        Column {
                            Text(
                                text = "学校",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            ExposedDropdownMenuBox(
                                expanded = schoolExpanded,
                                onExpandedChange = { schoolExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = schoolId,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = { 
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolExpanded) 
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.School, contentDescription = null, tint = RoseRed)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RoseRed,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = schoolExpanded,
                                    onDismissRequest = { schoolExpanded = false }
                                ) {
                                    schoolOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = { 
                                                schoolId = option
                                                schoolExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Campus Dropdown
                        var campusExpanded by remember { mutableStateOf(false) }
                        Column {
                            Text(
                                text = "校区",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            ExposedDropdownMenuBox(
                                expanded = campusExpanded,
                                onExpandedChange = { campusExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = campusId,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = { 
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = campusExpanded) 
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoseRed)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RoseRed,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = campusExpanded,
                                    onDismissRequest = { campusExpanded = false }
                                ) {
                                    campusOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = { 
                                                campusId = option
                                                campusExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Addresses Section
                    FormSection(
                        title = "地址管理",
                        icon = Icons.Default.LocationOn
                    ) {
                        EnhancedAddressesManager(
                            addresses = addresses,
                            currentAddress = currentAddress,
                            onCurrentAddressChanged = { currentAddress = it },
                            onAddressAdded = { newAddr ->
                                if (newAddr.isNotBlank() && !addresses.contains(newAddr)) {
                                    addresses = addresses + newAddr
                                }
                            },
                            onAddressRemoved = { addr ->
                                addresses = addresses.filter { it != addr }
                                if (currentAddress == addr) {
                                    currentAddress = addresses.firstOrNull() ?: ""
                                }
                            }
                        )
                    }
                    
                    // Bottom padding for scrolling
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun FormSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
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
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
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
            text = label,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
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
                focusedBorderColor = RoseRed,
                unfocusedBorderColor = Color.Gray
            )
        )
    }
}

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
                    focusedBorderColor = RoseRed,
                    unfocusedBorderColor = Color.Gray
                )
            )
            
            if (addresses.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    addresses.forEach { address ->
                        DropdownMenuItem(
                            text = { Text(address) },
                            onClick = { 
                                onCurrentAddressChanged(address)
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (address == currentAddress) RoseRed else Color.Gray
                                )
                            }
                        )
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
                                    text = address,
                                    modifier = Modifier.weight(1f)
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
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
                    focusedBorderColor = RoseRed,
                    unfocusedBorderColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    onAddressAdded(newAddress.trim())
                    newAddress = ""
                },
                enabled = newAddress.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoseRed,
                    disabledContainerColor = Color.LightGray
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("添加")
            }
        }
    }
}