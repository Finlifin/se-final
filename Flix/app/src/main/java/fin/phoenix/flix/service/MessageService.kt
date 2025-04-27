package fin.phoenix.flix.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import fin.phoenix.flix.MainActivity
import fin.phoenix.flix.R
import fin.phoenix.flix.api.ConnectionState
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageTypes
import fin.phoenix.flix.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MessageService : Service() {
    private val TAG = "MessageService"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "message_service"
    private val CHANNEL_NAME = "消息服务"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var messageRepository: MessageRepository

    override fun onCreate() {
        super.onCreate()
        messageRepository = MessageRepository.getInstance(this)
        createNotificationChannels()
        setupNetworkCallback()
        startForegroundService()
        setupMessageObservers()
        startPeriodicSync()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 从 SharedPreferences 获取认证信息
        val sharedPrefs = getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", "")
        val token = sharedPrefs.getString("auth_token", null)

        if (!token.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            // 连接 WebSocket 并开始同步
            serviceScope.launch {
                try {
                    // 初始化WebSocket连接
                    PhoenixMessageClient.instance.connect(token, userId)
                    // 执行初始同步
                    messageRepository.sync()
                } catch (e: Exception) {
                    Log.e(TAG, "初始化失败", e)
                    updateNotification("连接失败: ${e.message}")
                }
            }
        }

        return START_STICKY
    }

    private fun setupMessageObservers() {
        // 观察新消息
        messageRepository.newMessages
            .onEach { message -> handleNewMessage(message) }
            .catch { e -> Log.e(TAG, "处理新消息时出错", e) }
            .launchIn(serviceScope)

        // 观察连接状态
        PhoenixMessageClient.instance.connectionState
            .observeForever { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        updateNotification("消息服务运行中")
                        // 连接成功后执行同步
                        serviceScope.launch {
                            messageRepository.sync()
                        }
                    }
                    ConnectionState.CONNECTING -> {
                        updateNotification("正在连接...")
                    }
                    ConnectionState.DISCONNECTED -> {
                        updateNotification("连接已断开")
                    }
                    ConnectionState.CONNECTION_ERROR -> {
                        updateNotification("连接错误")
                    }
                    else -> {}
                }
            }

        // 观察错误
        PhoenixMessageClient.instance.errors
            .onEach { error ->
                Log.e(TAG, "WebSocket错误: ${error.code} - ${error.message}")
                updateNotification("连接错误: ${error.message}")
            }
            .catch { e -> Log.e(TAG, "处理错误时出错", e) }
            .launchIn(serviceScope)
    }

    private fun startPeriodicSync() {
        serviceScope.launch {
            while (true) {
                try {
                    delay(5 * 60 * 1000) // 5分钟同步一次
                    if (PhoenixMessageClient.instance.connectionState.value == ConnectionState.CONNECTED) {
                        messageRepository.sync()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "定期同步失败", e)
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 服务通知渠道 - 低优先级，静音
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持消息服务运行"
                setShowBadge(false)
            }

            // 消息通知渠道 - 默认优先级，有声音
            val messageChannel = NotificationChannel(
                "messages",
                "消息通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "新消息提醒"
                setShowBadge(true)
            }

            // 系统通知渠道 - 高优先级
            val systemChannel = NotificationChannel(
                "system",
                "系统通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "重要的系统通知"
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(
                listOf(serviceChannel, messageChannel, systemChannel)
            )
        }
    }

    private fun createNotification(content: String = "消息服务运行中"): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flix")
            .setContentText(content)
            .setSmallIcon(R.drawable.icons8_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun setupNetworkCallback() {
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "网络已恢复")
                // 网络恢复时重新连接
                val sharedPrefs = getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
                val userId = sharedPrefs.getString("user_id", "")
                val token = sharedPrefs.getString("auth_token", null)
                if (!token.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                    serviceScope.launch {
                        try {
                            PhoenixMessageClient.instance.connect(token, userId)
                            messageRepository.sync()
                        } catch (e: Exception) {
                            Log.e(TAG, "重连失败", e)
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "网络已断开")
                updateNotification("等待网络连接...")
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun handleNewMessage(message: Message) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // 确定通知渠道
        val channelId = when(message.messageType) {
            MessageTypes.SYSTEM_NOTIFICATION,
            MessageTypes.SYSTEM_ANNOUNCEMENT -> "system"
            else -> "messages"
        }
        
        // 创建消息通知
        val intent = when(message.messageType) {
            MessageTypes.PRIVATE_MESSAGE -> Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_CHAT
                putExtra("conversation_id", message.conversationId)
            }
            MessageTypes.SYSTEM_NOTIFICATION -> Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_NOTIFICATIONS
            }
            MessageTypes.SYSTEM_ANNOUNCEMENT -> Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_ANNOUNCEMENTS
            }
            else -> Intent(this, MainActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(when(message.messageType) {
                MessageTypes.PRIVATE_MESSAGE -> "新消息"
                MessageTypes.SYSTEM_NOTIFICATION -> "系统通知"
                MessageTypes.SYSTEM_ANNOUNCEMENT -> "系统公告"
                MessageTypes.INTERACTION -> "互动消息"
                else -> "新消息"
            })
            .setContentText(message.content.firstOrNull()?.payload?.toString() ?: "")
            .setSmallIcon(R.drawable.icons8_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(message.id.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止所有协程
        serviceScope.cancel()
        // 注销网络回调
        connectivityManager.unregisterNetworkCallback(networkCallback)
        // 断开 WebSocket 连接
        PhoenixMessageClient.instance.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}