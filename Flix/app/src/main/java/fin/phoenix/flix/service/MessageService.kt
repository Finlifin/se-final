package fin.phoenix.flix.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import fin.phoenix.flix.api.PhoenixMessageClient
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.repository.MessageRepository
import fin.phoenix.flix.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MessageService : Service() {
    companion object {
        const val TAG = "MessageService"
        const val NOTIFICATION_ID = NotificationHelper.NOTIFICATION_ID_SERVICE
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var messageRepository: MessageRepository
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        messageRepository = MessageRepository.getInstance(this)
        notificationHelper = NotificationHelper(this)

        // 创建通知渠道
        notificationHelper.createNotificationChannels()

        setupNetworkCallback()
        startForegroundService()
        setupMessageObservers()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notificationHelper.createServiceNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        } else {
            startForeground(NOTIFICATION_ID, notificationHelper.createServiceNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 从 SharedPreferences 获取认证信息
        val sharedPrefs = getSharedPreferences("flix_prefs", MODE_PRIVATE)
        val token = sharedPrefs.getString("auth_token", null)

        if (!token.isNullOrEmpty()) {
            Log.d(TAG, "服务启动，初始化WebSocket连接")
            serviceScope.launch {
                try {
                    // 通过MessageRepository初始化WebSocket连接
                    messageRepository.establish()
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
        messageRepository.messageClient.newMessages.onEach { message -> handleNewMessage(message) }
            .catch { e -> Log.e(TAG, "处理新消息时出错", e) }.launchIn(serviceScope)
    }

    private fun updateNotification(content: String) {
        notificationHelper.updateServiceNotification(content)
    }

    private fun setupNetworkCallback() {
        connectivityManager = getSystemService(ConnectivityManager::class.java)

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "网络已恢复")
                messageRepository.establish()
//                // 网络恢复时重新连接
//                val sharedPrefs = getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
//                val userId = sharedPrefs.getString("user_id", "")
//                val token = sharedPrefs.getString("auth_token", null)
//                if (!token.isNullOrEmpty() && !userId.isNullOrEmpty()) {
//                    serviceScope.launch {
//                        try {
//                            // 使用MessageRepository进行重连
//                            messageRepository.sync()
//                        } catch (e: Exception) {
//                            Log.e(TAG, "重连失败", e)
//                        }
//                    }
//                }
            }

            override fun onLost(network: Network) {
//                super.onLost(network)
//                Log.d(TAG, "网络已断开")
//                updateNotification("等待网络连接...")
            }
        }

        val networkRequest =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun handleNewMessage(message: Message) {
        val currentUserId = messageRepository.messageClient.userId

        if (message.senderId == currentUserId) {
            // 如果消息是自己发送的，直接返回
            return
        }
        // 使用新的NotificationHelper来处理消息通知
        notificationHelper.showMessageNotification(message)
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