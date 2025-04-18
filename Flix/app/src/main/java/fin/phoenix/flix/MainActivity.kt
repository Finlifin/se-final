package fin.phoenix.flix

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fin.phoenix.flix.api.ConnectionState
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.ui.about.AboutScreen
import fin.phoenix.flix.ui.home.HomeScreen
import fin.phoenix.flix.ui.login.SignInSignUpScreen
import fin.phoenix.flix.ui.message.ChatScreen
import fin.phoenix.flix.ui.message.MessageCenterScreen
import fin.phoenix.flix.ui.message.SystemNotificationScreen
import fin.phoenix.flix.ui.myprofile.MyProductsScreen
import fin.phoenix.flix.ui.myprofile.MyProfileContent
import fin.phoenix.flix.ui.myprofile.MyPurchasedProductsScreen
import fin.phoenix.flix.ui.myprofile.MySoldProductsScreen
import fin.phoenix.flix.ui.myprofile.ProfileEditScreen
import fin.phoenix.flix.ui.orders.OrderDetailScreen
import fin.phoenix.flix.ui.orders.OrderListScreen
import fin.phoenix.flix.ui.payment.PaymentConfirmScreen
import fin.phoenix.flix.ui.privacy.PrivacyPolicyScreen
import fin.phoenix.flix.ui.product.ProductDetailScreen
import fin.phoenix.flix.ui.product.ProductEditScreen
import fin.phoenix.flix.ui.product.PublishScreen
import fin.phoenix.flix.ui.profile.UserScreen
import fin.phoenix.flix.ui.settings.SettingsScreen
import fin.phoenix.flix.ui.theme.FlixTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    companion object {
        // Intent actions for notifications
        const val ACTION_OPEN_CHAT = "fin.phoenix.flix.action.OPEN_CHAT"
        const val ACTION_OPEN_NOTIFICATIONS = "fin.phoenix.flix.action.OPEN_NOTIFICATIONS"
        const val ACTION_OPEN_ANNOUNCEMENTS = "fin.phoenix.flix.action.OPEN_ANNOUNCEMENTS"
    }

    private fun initPhoenixClient() {
        val sharedPrefs = this.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", "")
        val token = sharedPrefs.getString("auth_token", null)

        if (!token.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            // 配置WebSocket URL（如果需要）
            // PhoenixMessageClient.instance.configureUrl("ws://your-server-url/socket/websocket")

            // 连接WebSocket服务器
            PhoenixMessageClient.instance.connect(token, userId)

            // 监听连接状态变化
            lifecycleScope.launch {
                PhoenixMessageClient.instance.connectionState.collectLatest { state ->
                    when (state) {
                        ConnectionState.CONNECTED -> {
                            Log.d(TAG, "WebSocket连接成功")
                            // 连接成功后加入用户频道
                            PhoenixMessageClient.instance.joinUserChannel().onSuccess {
                                    Log.d(TAG, "成功加入用户频道")
                                    // 启用自动同步消息
                                    PhoenixMessageClient.instance.enableAutoSync()
                                }.onFailure { error ->
                                    Log.e(TAG, "加入用户频道失败: ${error.message}")
                                }
                        }

                        ConnectionState.CONNECTION_ERROR -> {
                            Log.e(TAG, "WebSocket连接错误")
                        }

                        ConnectionState.DISCONNECTED -> {
                            Log.d(TAG, "WebSocket已断开连接")
                        }

                        ConnectionState.CONNECTING -> {
                            Log.d(TAG, "WebSocket正在连接...")
                        }

                        ConnectionState.RECONNECTING -> {
                            Log.d(TAG, "WebSocket正在重连...")
                        }
                    }
                }
            }

            // 监听WebSocket错误
            lifecycleScope.launch {
                PhoenixMessageClient.instance.errors.collect { error ->
                    Log.e(TAG, "WebSocket错误: ${error.code} - ${error.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "消息连接错误: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // 监听未读消息计数
            lifecycleScope.launch {
                PhoenixMessageClient.instance.unreadCounts.collect { counts ->
                    Log.d(TAG, "未读消息: 共${counts.total}条")
                    // 可以在这里更新UI上的未读消息角标
                }
            }
        } else {
            Log.w(TAG, "未找到用户ID或token，跳过WebSocket连接")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this

        // install thread exception handler
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
        }

        initPhoenixClient()

        setContent {
            FlixTheme {
                Flix(ctx, intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 在活动销毁时断开WebSocket连接
        // 如果你希望即使应用在后台也保持连接，可以考虑使用Service
        PhoenixMessageClient.instance.disconnect()
    }
}

@Composable
fun Flix(ctx: Context, intent: Intent?) {
    val navController = rememberNavController()
    val sharedPrefs = ctx.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
    val currentUserId = sharedPrefs.getString("user_id", "")

    NavHost(navController = navController, startDestination = "/home") {
        composable("/login") {
            SignInSignUpScreen(
                onLoginSuccess = {
                    // Show a toast to indicate success
                    Toast.makeText(ctx, "登录成功！", Toast.LENGTH_SHORT).show()

                    // Start the message service
//                    val serviceIntent = Intent(ctx, MessageService::class.java)
//                    ctx.startService(serviceIntent)

                    // Navigate to home and clear the login page from back stack
                    navController.navigate("/home") {
                        popUpTo("/login") { inclusive = true }
                    }
                })
        }
        composable("/home") {
            HomeScreen(navController = navController) // 首页
        }
        composable("/product/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(navController = navController, productId = productId)
        }
        // Add route for editing products
        composable("/product/edit/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductEditScreen(navController = navController, productId = productId)
        }
        composable("/publish") {
            PublishScreen(navController = navController) // 发布商品页
        }
        composable("/profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserScreen(navController = navController, userId = userId)
        }
        // Add new routes for MyProfile functionality
        composable("/my_profile") {
            if (currentUserId.isNullOrEmpty()) {
                Log.d("NavController", "User ID is null or empty, navigating to login")
                Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                navController.navigate("/login")
            }
            MyProfileContent(navController = navController, userId = currentUserId!!)
        }
        composable("/my_profile/my_products") {
            if (currentUserId.isNullOrEmpty()) {
                Log.d("NavController", "User ID is null or empty, navigating to login")
                Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                navController.navigate("/login")
            }
            MyProductsScreen(navController = navController, userId = currentUserId!!)
        }
        composable("/my_profile/sold_products") {
            if (currentUserId.isNullOrEmpty()) {
                Log.d("NavController", "User ID is null or empty, navigating to login")
                Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                navController.navigate("/login")
            }
            MySoldProductsScreen(navController = navController, userId = currentUserId!!)
        }
        composable("/my_profile/purchased_products") {
            if (currentUserId.isNullOrEmpty()) {
                Log.d("NavController", "User ID is null or empty, navigating to login")
                Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                navController.navigate("/login")
            }
            MyPurchasedProductsScreen(navController = navController, userId = currentUserId!!)
        }

        composable("/messages") {
            MessageCenterScreen(navController = navController)
        }

        composable("/messages/{targetUserId}") { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
            ChatScreen(navController = navController, partnerUserId = targetUserId)
        }

        // 添加系统通知路由
        composable("/notifications/system") {
            SystemNotificationScreen(
                navController = navController, conversationId = "system_notification"
            )
        }

        // 添加系统公告路由
        composable("/notifications/announcement") {
            SystemNotificationScreen(
                navController = navController, conversationId = "system_announcement"
            )
        }

        // 添加互动消息路由
        composable("/notifications/interaction") {
            SystemNotificationScreen(navController = navController, conversationId = "interaction")
        }

        composable("/settings") {
            SettingsScreen(navController)
        }

        composable("/about") {
            AboutScreen(navController)
        }

        composable("/privacy_policy") {
            PrivacyPolicyScreen(navController)
        }

        composable("/my_profile/edit") {
            if (currentUserId.isNullOrEmpty()) {
                Log.d("NavController", "User ID is null or empty, navigating to login")
                Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                navController.navigate("/login")
            }
            ProfileEditScreen(navController, currentUserId!!)
        }

        // 添加支付确认页面路由
        composable(
            route = "/payment/confirm/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            PaymentConfirmScreen(navController = navController, orderId = orderId)
        }

        // 添加订单详情页面路由
        composable(
            route = "/orders/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(navController = navController, orderId = orderId)
        }

        // 添加订单列表页面路由
        composable(route = "/orders") {
            OrderListScreen(navController = navController)
        }
    }
}

private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
    // 1. 记录日志 (非常重要)
    // 使用 Logcat 或更专业的日志库 (如 Timber, Crashlytics)
    Log.e("MyAppCrash", "Uncaught exception in thread ${thread.name}", throwable)

    // 2. (可选) 上报错误到后台服务
    // 例如 Firebase Crashlytics, Sentry 等
    // FirebaseCrashlytics.getInstance().recordException(throwable)

    // 3. (可选) 尝试给用户一个友好的提示并退出
    // 注意：此时应用状态可能不稳定，启动新 Activity 有风险，但通常比直接崩溃好
    // 需要确保这个 Activity 足够简单和健壮
//    try {
//        val intent = Intent(applicationContext, CrashActivity::class.java).apply {
//            // 清除任务栈并创建一个新任务
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            putExtra(CrashActivity.EXTRA_ERROR_MESSAGE, throwable.localizedMessage ?: "Unknown error")
//            putExtra(CrashActivity.EXTRA_ERROR_STACKTRACE, Log.getStackTraceString(throwable)) // 可选，用于调试
//        }
//        applicationContext.startActivity(intent)
//    } catch (e: Exception) {
//        Log.e("MyAppCrash", "Failed to start CrashActivity", e)
//        // 如果启动 CrashActivity 也失败了，至少日志已经记录
//    }

    // 4. 结束当前进程
    // 调用原始的处理器（如果有特殊需求，比如某些库设置了自己的处理器）
    // defaultExceptionHandler?.uncaughtException(thread, throwable)
    // 或者直接退出进程
//    Process.killProcess(Process.myPid()) // 强制杀掉进程
//    exitProcess(1) // 退出 JVM，参数非 0 表示异常退出
}
