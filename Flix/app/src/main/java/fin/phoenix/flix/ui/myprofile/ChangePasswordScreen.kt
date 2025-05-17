package fin.phoenix.flix.ui.myprofile

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.repository.AuthRepository
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.login.CustomTextField
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    val userManager = UserManager.getInstance(context)

    // 用于标识用户是否有密码（影响UI展示和API调用）
    // 这里应该通过ViewModel或其他方式获取用户是否已有密码的信息
    var hasExistingPassword by remember { mutableStateOf(true) }
    
    // 模式切换 - 修改密码/设置密码
    var passwordMode by remember { mutableStateOf(if (hasExistingPassword) "修改密码" else "设置密码") }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // 短信验证相关（用于无密码用户）
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var showSmsOption by remember { mutableStateOf(!hasExistingPassword) }
    
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 密码强度提示
    var passwordStrength by remember { mutableStateOf<String?>(null) }
    var passwordStrengthColor by remember { mutableStateOf(Color.Gray) }

    // 检查新密码强度
    fun checkPasswordStrength(password: String) {
        if (password.isEmpty()) {
            passwordStrength = null
            return
        }

        if (password.length < 6 || password.length > 16) {
            passwordStrength = "密码长度应为6-16位字符"
            passwordStrengthColor = Color.Red
            return
        }

        var containsDigit = false
        var containsUpperCase = false
        var containsLowerCase = false
        var containsSpecial = false
        
        for (char in password) {
            when {
                char.isDigit() -> containsDigit = true
                char.isUpperCase() -> containsUpperCase = true
                char.isLowerCase() -> containsLowerCase = true
                !char.isLetterOrDigit() -> containsSpecial = true
            }
        }
        
        val categoryCount = listOf(containsDigit, containsUpperCase, containsLowerCase, containsSpecial)
            .count { it }
            
        when {
            categoryCount == 1 -> {
                passwordStrength = "弱密码（仅包含一种字符类型）"
                passwordStrengthColor = Color.Red
            }
            categoryCount == 2 -> {
                passwordStrength = "中等密码（包含两种字符类型）"
                passwordStrengthColor = Color(0xFFFFA500) // 橙色
            }
            categoryCount == 3 -> {
                passwordStrength = "强密码（包含三种字符类型）"
                passwordStrengthColor = Color(0xFF008000) // 绿色
            }
            else -> {
                passwordStrength = "非常强的密码（包含所有四种字符类型）"
                passwordStrengthColor = Color(0xFF006400) // 深绿色
            }
        }
    }
    
    // 发送短信验证码
    fun sendSmsCode() {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "请输入手机号", Toast.LENGTH_SHORT).show()
            return
        }
        
        coroutineScope.launch {
            isLoading = true
            val result = authRepository.sendSmsCode(phoneNumber)
            isLoading = false
            
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(context, result.message ?: "发送验证码失败", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(context, "发送验证码出现未知错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(passwordMode) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 切换模式 - 修改密码/设置密码
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            passwordMode = "修改密码"
                            showSmsOption = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (passwordMode == "修改密码") RoseRed else Color.LightGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("修改密码")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            passwordMode = "设置密码"
                            showSmsOption = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (passwordMode == "设置密码") RoseRed else Color.LightGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("设置密码")
                    }
                }
                
                if (showSmsOption) {
                    // 手机号输入框（仅在设置密码模式下显示）
                    CustomTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = "手机号码",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    
                    // 短信验证码输入框和获取验证码按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomTextField(
                            value = smsCode,
                            onValueChange = { smsCode = it },
                            label = "验证码",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { sendSmsCode() },
                            enabled = !isLoading && phoneNumber.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                        ) {
                            Text("获取验证码")
                        }
                    }
                } else {
                    // 旧密码输入框（仅在修改密码模式下显示）
                    CustomTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = "当前密码",
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                modifier = Modifier.clickable { oldPasswordVisible = !oldPasswordVisible }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                }
                
                // 新密码输入框
                CustomTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it
                        // 检查密码强度
                        checkPasswordStrength(it) 
                    },
                    label = "新密码",
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "New Password")
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            modifier = Modifier.clickable { newPasswordVisible = !newPasswordVisible }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )
                
                // 密码强度提示
                passwordStrength?.let {
                    Text(
                        text = it,
                        color = passwordStrengthColor,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
                
                // 密码要求提示
                Text(
                    text = "密码要求：6-16位字符，必须包含数字、大写字母、小写字母、特殊符号中的至少两种",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                // 确认密码输入框
                CustomTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "确认新密码",
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            modifier = Modifier.clickable { confirmPasswordVisible = !confirmPasswordVisible }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )
                
                // 错误信息显示
                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 确认按钮
                Button(
                    onClick = {
                        // 根据当前模式执行相应的密码操作
                        if (passwordMode == "修改密码") {
                            // 修改已有密码
                            if (!validateUpdateInputs(context, oldPassword, newPassword, confirmPassword)) {
                                return@Button
                            }
                            
                            isLoading = true
                            errorMessage = null
                            
                            coroutineScope.launch {
                                val result = authRepository.updatePassword(oldPassword, newPassword)
                                isLoading = false
                                
                                when (result) {
                                    is Resource.Success -> {
                                        Toast.makeText(context, "密码修改成功，请重新登录", Toast.LENGTH_LONG).show()
                                        // 清除登录状态
                                        userManager.clearCurrentUser()
                                        // 跳转到登录页面，并清除回退栈
                                        navController.navigate("/login") {
                                            popUpTo("/home") { inclusive = true }
                                        }
                                    }
                                    is Resource.Error -> {
                                        errorMessage = result.message
                                    }
                                    else -> {
                                        errorMessage = "修改密码时出现未知错误"
                                    }
                                }
                            }
                        } else {
                            // 设置初始密码
                            if (showSmsOption) {
                                // 使用短信验证码设置密码
                                if (!validateSetPasswordInputs(context, phoneNumber, smsCode, newPassword, confirmPassword)) {
                                    return@Button
                                }
                                
                                isLoading = true
                                errorMessage = null
                                
                                coroutineScope.launch {
                                    val result = authRepository.setInitialPasswordWithSms(phoneNumber, smsCode, newPassword)
                                    isLoading = false
                                    
                                    when (result) {
                                        is Resource.Success -> {
                                            Toast.makeText(context, "密码设置成功，请登录", Toast.LENGTH_LONG).show()
                                            // 清除登录状态
                                            userManager.clearCurrentUser()
                                            // 跳转到登录页面，并清除回退栈
                                            navController.navigate("/login") {
                                                popUpTo("/home") { inclusive = true }
                                            }
                                        }
                                        is Resource.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> {
                                            errorMessage = "设置密码时出现未知错误"
                                        }
                                    }
                                }
                            } else {
                                // 使用当前登录状态设置密码
                                if (!validateNewPasswordInputs(context, newPassword, confirmPassword)) {
                                    return@Button
                                }
                                
                                isLoading = true
                                errorMessage = null
                                
                                coroutineScope.launch {
                                    val result = authRepository.setInitialPassword(newPassword)
                                    isLoading = false
                                    
                                    when (result) {
                                        is Resource.Success -> {
                                            Toast.makeText(context, "密码设置成功，请重新登录", Toast.LENGTH_LONG).show()
                                            // 清除登录状态
                                            userManager.clearCurrentUser()
                                            // 跳转到登录页面，并清除回退栈
                                            navController.navigate("/login") {
                                                popUpTo("/home") { inclusive = true }
                                            }
                                        }
                                        is Resource.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> {
                                            errorMessage = "设置密码时出现未知错误"
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (passwordMode == "修改密码") "确认修改" else "确认设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 验证修改密码的输入
private fun validateUpdateInputs(
    context: Context,
    oldPassword: String,
    newPassword: String,
    confirmPassword: String
): Boolean {
    // 验证旧密码
    if (oldPassword.isEmpty()) {
        Toast.makeText(context, "请输入当前密码", Toast.LENGTH_SHORT).show()
        return false
    }
    
    return validateNewPasswordInputs(context, newPassword, confirmPassword)
}

// 验证设置初始密码的输入（使用短信验证码）
private fun validateSetPasswordInputs(
    context: Context,
    phoneNumber: String,
    smsCode: String,
    newPassword: String,
    confirmPassword: String
): Boolean {
    // 验证手机号
    if (phoneNumber.isEmpty()) {
        Toast.makeText(context, "请输入手机号", Toast.LENGTH_SHORT).show()
        return false
    }
    
    // 验证手机号格式
    if (!phoneNumber.matches(Regex("^1\\d{10}$"))) {
        Toast.makeText(context, "手机号格式不正确", Toast.LENGTH_SHORT).show()
        return false
    }
    
    // 验证验证码
    if (smsCode.isEmpty()) {
        Toast.makeText(context, "请输入验证码", Toast.LENGTH_SHORT).show()
        return false
    }
    
    return validateNewPasswordInputs(context, newPassword, confirmPassword)
}

// 验证新密码
private fun validateNewPasswordInputs(
    context: Context,
    newPassword: String,
    confirmPassword: String
): Boolean {
    // 验证新密码
    if (newPassword.isEmpty()) {
        Toast.makeText(context, "请输入新密码", Toast.LENGTH_SHORT).show()
        return false
    }
    
    // 验证新密码长度
    if (newPassword.length < 6 || newPassword.length > 16) {
        Toast.makeText(context, "新密码长度应为6-16位字符", Toast.LENGTH_SHORT).show()
        return false
    }
    
    // 验证新密码复杂度
    val containsDigit = newPassword.any { it.isDigit() }
    val containsUpperCase = newPassword.any { it.isUpperCase() }
    val containsLowerCase = newPassword.any { it.isLowerCase() }
    val containsSpecial = newPassword.any { !it.isLetterOrDigit() }
    
    val categoryCount = listOf(containsDigit, containsUpperCase, containsLowerCase, containsSpecial)
        .count { it }
        
    if (categoryCount < 2) {
        Toast.makeText(context, "密码必须包含数字、大写字母、小写字母、特殊字符中的至少两种", Toast.LENGTH_SHORT).show()
        return false
    }
    
    // 验证两次输入的新密码是否一致
    if (newPassword != confirmPassword) {
        Toast.makeText(context, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
        return false
    }
    
    return true
}