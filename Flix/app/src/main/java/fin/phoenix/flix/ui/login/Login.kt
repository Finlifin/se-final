package fin.phoenix.flix.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fin.phoenix.flix.repository.AuthRepository
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SignInSignUpScreen(onLoginSuccess: () -> Unit = {}) {
    val context = LocalContext.current

    // 获取协程作用域
    val coroutineScope = rememberCoroutineScope()


    // Login form states
    var isPasswordLogin by remember { mutableStateOf(true) }
    var phoneNumber by remember { mutableStateOf("11451419191") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("121212") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }


    // Verification code countdown state
    var countdown by remember { mutableIntStateOf(0) }
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Main content card
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .align(Alignment.Center)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Login/Register Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TabButton(
                        title = "密码登录",
                        isSelected = isPasswordLogin,
                        onClick = { isPasswordLogin = true })
                    TabButton(
                        title = "验证码登录",
                        isSelected = !isPasswordLogin,
                        onClick = { isPasswordLogin = false })
                }

                // Form fields
                // Phone number field
                CustomTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "手机号",
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Container for the animated content with a consistent height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // Increased height to accommodate both forms comfortably
                ) {
                    Column {
                        AnimatedVisibility(
                            visible = isPasswordLogin,
                            enter = slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Password login form
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Password field
                                CustomTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = "密码",
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Lock, contentDescription = "Password"
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            modifier = Modifier.clickable {
                                                passwordVisible = !passwordVisible
                                            })
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Forgot password link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "忘记密码?",
                                        color = RoseRed,
                                        modifier = Modifier.clickable {
                                            Toast.makeText(
                                                context, "忘记密码功能暂未实现", Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !isPasswordLogin,
                            enter = slideInHorizontally(initialOffsetX = { -it }) + androidx.compose.animation.fadeIn(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Verification code form
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CustomTextField(
                                    value = verificationCode,
                                    onValueChange = { verificationCode = it },
                                    label = "验证码",
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Verification Code"
                                        )
                                    },
                                    trailingIcon = {
                                        Button(
                                            onClick = {
                                                if (countdown == 0) {
                                                    countdown = 60
                                                    Toast.makeText(
                                                        context, "验证码已发送", Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            enabled = countdown == 0,
                                            modifier = Modifier.padding(4.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                                            contentPadding = PaddingValues(
                                                horizontal = 12.dp, vertical = 6.dp
                                            )
                                        ) {
                                            Text(
                                                text = if (countdown > 0) "${countdown}s" else "获取验证码",
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Registration hint
                                Text(
                                    text = "如果该手机号没有注册过，会自动注册",
                                    color = RoseRed,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login/Register Button
                Button(
                    onClick = {
                        if (validateInputs(context, phoneNumber, password, verificationCode, isPasswordLogin)) {
                            isLoading = true

                            val authRepository = AuthRepository(context)

                            // 使用协程执行登录请求
                            coroutineScope.launch {
                                val result = if (isPasswordLogin) {
                                    authRepository.loginWithPassword(phoneNumber, password)
                                } else {
                                    authRepository.loginWithSms(phoneNumber, verificationCode)
                                }

                                isLoading = false

                                when (result) {
                                    is Resource.Success -> {
                                        // 保存 token 和用户信息到 SharedPreferences
                                        val sharedPref = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
                                        with(sharedPref.edit()) {
                                            putString("auth_token", result.data.token)
                                            putString("user_id", result.data.user.uid)
                                            putString("user_name", result.data.user.userName)
                                            apply()
                                        }
                                        Log.d("Login", "Login successful for user: ${result.data.user.userName}")
                                        onLoginSuccess()
                                    }
                                    is Resource.Error -> {
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                    is Resource.Loading -> {
//                                        Toast.makeText(context, "登录中...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isPasswordLogin) "登录" else "验证并登录",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Additional login methods placeholder
                Text(
                    text = "其他登录方式 (暂未实现)",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
