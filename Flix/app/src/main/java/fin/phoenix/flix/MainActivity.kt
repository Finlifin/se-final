package fin.phoenix.flix

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fin.phoenix.flix.api.ConnectionState
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.UserManager
import fin.phoenix.flix.service.MessageService
import fin.phoenix.flix.ui.about.AboutScreen
import fin.phoenix.flix.ui.home.HomeScreen
import fin.phoenix.flix.ui.login.SignInSignUpScreen
import fin.phoenix.flix.ui.message.ChatScreen
import fin.phoenix.flix.ui.message.ChatSettingScreen
import fin.phoenix.flix.ui.message.MessageCenterScreen
import fin.phoenix.flix.ui.message.SystemNotificationScreen
import fin.phoenix.flix.ui.myprofile.MyFavoritesScreen
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
import fin.phoenix.flix.ui.search.SearchScreen
import fin.phoenix.flix.ui.settings.SettingsScreen
import fin.phoenix.flix.ui.theme.FlixTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val context: Context = this
    private lateinit var userManager: UserManager

    companion object {
        // Intent actions for notifications
        const val ACTION_OPEN_CHAT = "fin.phoenix.flix.action.OPEN_CHAT"
        const val ACTION_OPEN_NOTIFICATIONS = "fin.phoenix.flix.action.OPEN_NOTIFICATIONS"
        const val ACTION_OPEN_ANNOUNCEMENTS = "fin.phoenix.flix.action.OPEN_ANNOUNCEMENTS"
    }

    private fun startMessageService() {
        // 在Android 13+上请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
        val serviceIntent = Intent(this, MessageService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun stopMessageService() {
        val serviceIntent = Intent(this, MessageService::class.java)
        stopService(serviceIntent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    private fun initPhoenixClient() {
        val sharedPrefs = this.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
        val userId = userManager.currentUserId.value
        val token = sharedPrefs.getString("auth_token", null)

        if (!token.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            // 配置WebSocket URL（如果需要）
            // PhoenixMessageClient.instance.configureUrl("ws://your-server-url/socket/websocket")

            // 连接WebSocket服务器
            PhoenixMessageClient.instance.connect(token, userId)
            // 同步消息

            // 监听连接状态变化
            PhoenixMessageClient.instance.connectionState.observe(this, Observer { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        Log.d(TAG, "WebSocket连接成功")
                        // 连接成功后加入用户频道
                        lifecycleScope.launch {
                            PhoenixMessageClient.instance.joinUserChannel().onSuccess {
                                Log.d(TAG, "成功加入用户频道")
                                // 启用自动同步消息
                                PhoenixMessageClient.instance.enableAutoSync()
                            }.onFailure { error ->
                                Log.e(TAG, "加入用户频道失败: ${error.message}")
                            }
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
            })

            // 监听WebSocket错误
            lifecycleScope.launch {
                PhoenixMessageClient.instance.errors.collect { error ->
                    Log.e(TAG, "WebSocket错误: ${error.code} - ${error.message}")
                    Toast.makeText(
                        this@MainActivity, "消息连接错误: ${error.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // 监听未读消息计数
            PhoenixMessageClient.instance.unreadCounts.observe(this, Observer { counts ->
                Log.d(TAG, "未读消息: 共${counts.total}条")
                // 可以在这里更新UI上的未读消息角标
            })
        } else {
            Log.w(TAG, "未找到用户ID或token，跳过WebSocket连接")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this

        // 初始化UserManager
        userManager = UserManager.getInstance(this)

        // 检查用户是否已登录
        val sharedPrefs = getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
        val userId = userManager.currentUserId.value
        val token = sharedPrefs.getString("auth_token", "")

        // 只在用户已登录时启动消息服务和初始化Phoenix客户端
        if (!token.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            startMessageService()
            initPhoenixClient()
        } else {
            Log.d(TAG, "用户未登录，跳过启动消息服务和初始化客户端")
        }

        // install thread exception handler
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
        }

        setContent {
            FlixTheme {
                Flix(ctx, intent, userManager, ::initPhoenixClient, ::startMessageService)
            }
        }

        // 处理通知点击
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_CHAT -> {
                val conversationId = intent.getStringExtra("conversation_id")
                if (conversationId != null) {
                    // 导航到聊天界面的逻辑
                }
            }

            ACTION_OPEN_NOTIFICATIONS -> {
                // 导航到系统通知界面的逻辑
            }

            ACTION_OPEN_ANNOUNCEMENTS -> {
                // 导航到系统公告界面的逻辑
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMessageService()
        // 在活动销毁时断开WebSocket连接
        // 如果你希望即使应用在后台也保持连接，可以考虑使用Service
        PhoenixMessageClient.instance.disconnect()
    }
}

@Composable
fun Flix(
    ctx: Context,
    intent: Intent?,
    userManager: UserManager,
    initPhoenixClient: () -> Unit,
    startMessageService: () -> Unit
) {
    val navController = rememberNavController()
    // 使用UserManager获取当前用户ID
    val currentUserId by userManager.currentUserId.observeAsState()

    /**
     * 使用LaunchedEffect实现一次性导航重定向的Composable
     * @param route 当前路由
     * @return 如果用户已登录返回true，否则返回false
     */
    @Composable
    fun LoginRequiredScreen(
        route: String, content: @Composable () -> Unit
    ) {
        if (currentUserId == null) {
            // 使用LaunchedEffect确保导航操作只执行一次
            LaunchedEffect(route) {
                Log.d("NavController", "User ID is null or empty, navigating to login from $route")
                Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show()
                navController.navigate("/login")
            }
            // 不渲染任何内容，避免执行后续逻辑
        } else {
            // 用户已登录，显示实际内容
            content()
        }
    }

    NavHost(navController = navController, startDestination = "/home") {
        composable("/login") {
            SignInSignUpScreen(
                onLoginSuccess = {
                    // Show a toast to indicate success
                    Toast.makeText(ctx, "登录成功！", Toast.LENGTH_SHORT).show()

                    // Start the message service
                    startMessageService()

                    // Initialize Phoenix client
                    initPhoenixClient()

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
        composable("/product/edit/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductEditScreen(navController = navController, productId = productId)
        }
        composable("/publish") {
            LoginRequiredScreen(route = "/publish") {
                PublishScreen(navController = navController)
            }
        }
        composable("/profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserScreen(navController = navController, userId = userId)
        }
        composable("/my_profile") {
            LoginRequiredScreen(route = "/my_profile") {
                MyProfileContent(navController = navController, userId = currentUserId!!)
            }
        }
        composable("/my_profile/my_products") {
            LoginRequiredScreen(route = "/my_profile/my_products") {
                MyProductsScreen(navController = navController, userId = currentUserId!!)
            }
        }
        composable("/my_profile/sold_products") {
            LoginRequiredScreen(route = "/my_profile/sold_products") {
                MySoldProductsScreen(navController = navController, userId = currentUserId!!)
            }
        }
        composable("/my_profile/purchased_products") {
            LoginRequiredScreen(route = "/my_profile/purchased_products") {
                MyPurchasedProductsScreen(navController = navController, userId = currentUserId!!)
            }
        }
        composable("/my_profile/favorites") {
            LoginRequiredScreen(route = "/my_profile/favorites") {
                MyFavoritesScreen(navController = navController, userId = currentUserId!!)
            }
        }
        composable("/messages") {
            LoginRequiredScreen(route = "/messages") {
                MessageCenterScreen(navController = navController)
            }
        }
        composable("/messages/{conversationId}") { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val route = "/messages/$conversationId"
            LoginRequiredScreen(route = route) {
                ChatScreen(navController = navController, conversationId = conversationId)
            }
        }
        composable(
            route = "/messages/settings/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val route = "/messages/settings/$conversationId"
            LoginRequiredScreen(route = route) {
                ChatSettingScreen(navController = navController, conversationId = conversationId)
            }
        }
        composable("/notifications/system") {
            SystemNotificationScreen(
                navController = navController, conversationId = "system_notification"
            )
        }
        composable("/notifications/announcement") {
            LoginRequiredScreen(route = "/notifications/announcement") {
                SystemNotificationScreen(
                    navController = navController,
                    conversationId = "system_announcement:$currentUserId"
                )
            }
        }
        composable("/notifications/interaction") {
            LoginRequiredScreen(route = "/notifications/interaction") {
                SystemNotificationScreen(
                    navController = navController, conversationId = "interaction:${currentUserId}"
                )
            }
        }
        composable(route = "/search") {
            SearchScreen(navController = navController)
        }
        composable(
            route = "/search?query={query}", arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                })
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchScreen(navController = navController, initialQuery = query)
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
            LoginRequiredScreen(route = "/my_profile/edit") {
                ProfileEditScreen(navController, currentUserId!!)
            }
        }
        composable(
            route = "/payment/confirm/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val route = "/payment/confirm/$orderId"
            LoginRequiredScreen(route = route) {
                PaymentConfirmScreen(navController = navController, orderId = orderId)
            }
        }
        composable(
            route = "/orders/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val route = "/orders/$orderId"
            LoginRequiredScreen(route = route) {
                OrderDetailScreen(navController = navController, orderId = orderId)
            }
        }
        composable(route = "/orders") {
            LoginRequiredScreen(route = "/orders") {
                OrderListScreen(navController = navController)
            }
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
