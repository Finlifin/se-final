package fin.phoenix.flix.ui.myprofile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fin.phoenix.flix.api.ProfileUpdateRequest
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
    val avatarUpdateState by viewModel.avatarUpdateState.observeAsState(initial = null)

    var userName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addresses by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentAddress by remember { mutableStateOf("") }
    var schoolId by remember { mutableStateOf("") }
    var campusId by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isAvatarUploading by remember { mutableStateOf(false) }

    // 收集学校和校区数据流
    val schoolsState by viewModel.schoolsState.collectAsState()
    val campusesState by viewModel.campusesState.collectAsState()

    // 搜索学校状态
    var schoolSearchQuery by remember { mutableStateOf("") }
    var campusSearchQuery by remember { mutableStateOf("") }

    // 显示的学校和校区名称（用于UI展示）
    val schoolName by viewModel.schoolNameState.observeAsState(initial = Resource.Loading)
    val campusName by viewModel.campusNameState.observeAsState(initial = Resource.Loading)

    // 底部表单状态
    var showSchoolBottomSheet by remember { mutableStateOf(false) }
    var showCampusBottomSheet by remember { mutableStateOf(false) }

    // 新学校和新校区的对话框状态
    var showAddSchoolDialog by remember { mutableStateOf(false) }
    var showAddCampusDialog by remember { mutableStateOf(false) }

    // 新学校和新校区的输入状态
    var newSchoolName by remember { mutableStateOf("") }
    var newSchoolCode by remember { mutableStateOf("未知代码") }
    var newCampusName by remember { mutableStateOf("") }
    var newCampusAddress by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // 当搜索查询变化时执行即时搜索
    LaunchedEffect(schoolSearchQuery) {
        if (schoolSearchQuery.length > 1) {
            viewModel.searchSchools(schoolSearchQuery)
        } else if (schoolSearchQuery.isEmpty()) {
            viewModel.loadSchools()
        }
    }

    // Load user data when screen is first displayed
    LaunchedEffect(userId) {
        viewModel.getUserProfile(userId)
    }

    // Initialize fields when user data is loaded
    LaunchedEffect(userState) {
        if (userState is Resource.Success<ProfileUpdateRequest>) {
            val user = (userState as Resource.Success<ProfileUpdateRequest>).data
            userName = user.userName
            phone = user.phoneNumber
            addresses = user.addresses
            currentAddress = user.currentAddress ?: ""
            schoolId = user.schoolId ?: ""
            campusId = user.campusId ?: ""

            // 如果用户已选择学校，加载相应的校区
            if (!schoolId.isNullOrBlank() && schoolId != "未设置") {
                viewModel.loadCampuses(schoolId)
            }
        }
    }

    // 当学校ID变化时，加载对应的校区列表
    LaunchedEffect(schoolId) {
        if (!schoolId.isBlank() && schoolId != "未设置") {
            viewModel.loadCampuses(schoolId)
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

    // Handle avatar update state changes
    LaunchedEffect(avatarUpdateState) {
        when (avatarUpdateState) {
            is Resource.Success<*> -> {
                isAvatarUploading = false
                scope.launch {
                    snackbarHostState.showSnackbar("头像上传成功")
                }
            }

            is Resource.Error -> {
                isAvatarUploading = false
                scope.launch {
                    snackbarHostState.showSnackbar("头像上传失败: ${(avatarUpdateState as Resource.Error).message}")
                }
            }

            is Resource.Loading -> {
                isAvatarUploading = true
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
            // 上传头像到服务器
            if (userState is Resource.Success<ProfileUpdateRequest>) {
                val user = (userState as Resource.Success<ProfileUpdateRequest>).data
                viewModel.updateAvatar(user.uid, it) { success ->
                    if (!success) {
                        scope.launch {
                            snackbarHostState.showSnackbar("头像更新失败，请重试")
                        }
                    }
                }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("编辑个人资料") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        }, actions = {
            TextButton(
                onClick = {
                    isLoading = true
                    if (userState is Resource.Success<ProfileUpdateRequest>) {
                        val currentUser = (userState as Resource.Success<ProfileUpdateRequest>).data
                        val updatedUser = currentUser.copy(
                            userName = userName,
                            addresses = addresses,
                            currentAddress = currentAddress.takeIf { it.isNotBlank() && it != "未设置" },
                            schoolId = schoolId.takeIf { it.isNotBlank() && it != "未设置" },
                            campusId = campusId.takeIf { it.isNotBlank() && it != "未设置" })
                        viewModel.updateProfile(updatedUser) { success ->
                            if (success) {
                                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("保存失败，请重试")
                                }
                            }
                        }
                    }
                }, enabled = !isLoading && !isAvatarUploading && userName.isNotBlank()
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
        }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
        )
    }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
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
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RoseRed)
                    }
                }

                is Resource.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "加载失败：${(userState as Resource.Error).message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is Resource.Success -> {
                    val user = (userState as Resource.Success<ProfileUpdateRequest>).data

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
                                        .crossfade(true).build(),
                                    contentDescription = "头像",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )

                                // Edit icon overlay
                                IconButton(
                                    onClick = { if (!isAvatarUploading) imagePicker.launch("image/*") },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(RoseRed)
                                ) {
                                    if (isAvatarUploading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "更换头像",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (isAvatarUploading) "正在上传..." else "点击更换头像",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Form sections
                    FormSection(
                        title = "基本信息", icon = Icons.Default.Person
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
                        title = "学校信息", icon = Icons.Default.School
                    ) {
                        // School selector
                        Column {
                            Text(
                                text = "学校",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = schoolName.show(),
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSchoolBottomSheet = true },
                                placeholder = {
                                    Text(
                                        "点击选择学校",
                                        modifier = Modifier.clickable {
                                            showSchoolBottomSheet = true
                                        })
                                },
                                readOnly = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = RoseRed
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showSchoolBottomSheet = true }) {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "选择学校"
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campus selector
                        Column {
                            Text(
                                text = "校区",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = campusName.show(),
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !schoolId.isNullOrBlank() && schoolId != "未设置"
                                    ) {
                                        if (!schoolId.isNullOrBlank() && schoolId != "未设置") {
                                            showCampusBottomSheet = true
                                        }
                                    },
                                placeholder = { Text("请先选择学校") },
                                readOnly = true,
                                enabled = !schoolId.isNullOrBlank() && schoolId != "未设置",
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = RoseRed
                                    )
                                },
                                trailingIcon = {
                                    if (!schoolId.isNullOrBlank() && schoolId != "未设置") {
                                        IconButton(onClick = { showCampusBottomSheet = true }) {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "选择校区"
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Addresses Section
                    FormSection(
                        title = "地址管理", icon = Icons.Default.LocationOn
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
                            })
                    }

                    // Bottom padding for scrolling
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // 学校选择底部表单
    SchoolSelectionBottomSheet(
        isVisible = showSchoolBottomSheet,
        schoolSearchQuery = schoolSearchQuery,
        onSearchQueryChange = { schoolSearchQuery = it },
        schoolsState = schoolsState,
        onSchoolSelected = { id, name ->
            schoolId = id
            viewModel.setSchoolName(name)
            schoolSearchQuery = ""
            campusId = ""
            viewModel.setCampusName("未设置")
            showSchoolBottomSheet = false
        },
        onAddSchoolClick = {
            showAddSchoolDialog = true
            newSchoolName = ""
            newSchoolCode = ""
        },
        onDismiss = { showSchoolBottomSheet = false })

    // 校区选择底部表单
    CampusSelectionBottomSheet(
        isVisible = showCampusBottomSheet,
        schoolName = schoolName.show(),
        campusSearchQuery = campusSearchQuery,
        onSearchQueryChange = { campusSearchQuery = it },
        campusesState = campusesState,
        onCampusSelected = { id, name ->
            campusId = id
            viewModel.setCampusName(name)
            campusSearchQuery = ""
            showCampusBottomSheet = false
        },
        onAddCampusClick = {
            showAddCampusDialog = true
            newCampusName = ""
            newCampusAddress = ""
        },
        onDismiss = { showCampusBottomSheet = false })

    // 添加新学校对话框
    AddSchoolDialog(
        isVisible = showAddSchoolDialog,
        schoolName = newSchoolName,
        onSchoolNameChange = { newSchoolName = it },
        schoolCode = newSchoolCode,
        onSchoolCodeChange = { newSchoolCode = it },
        onAddClick = {
            viewModel.addSchool(newSchoolName, newSchoolCode) { success, newId, message ->
                if (success && newId != null) {
                    schoolId = newId
                    viewModel.setSchoolName(newSchoolName)
                    campusId = ""
                    viewModel.setCampusName("未设置")
                    showAddSchoolDialog = false
                    scope.launch {
                        Toast.makeText(context, "学校添加成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    scope.launch {
                        Toast.makeText(context, "添加失败: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        onDismiss = { showAddSchoolDialog = false })

    // 添加新校区对话框
    AddCampusDialog(
        isVisible = showAddCampusDialog,
        schoolName = schoolName.show(),
        campusName = newCampusName,
        onCampusNameChange = { newCampusName = it },
        campusAddress = newCampusAddress,
        onCampusAddressChange = { newCampusAddress = it },
        onAddClick = {
            viewModel.addCampus(
                schoolId, newCampusName, newCampusAddress
            ) { success, newId, message ->
                if (success && newId != null) {
                    campusId = newId
                    viewModel.setCampusName(newCampusName)
                    showAddCampusDialog = false
                    scope.launch {
                        Toast.makeText(context, "校区添加成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    scope.launch {
                        Toast.makeText(context, "添加失败: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        onDismiss = { showAddCampusDialog = false })
}

fun Resource<String>.show(): String {
    return when (this) {
        is Resource.Success -> this.data
        else -> "未设置"
    }
}