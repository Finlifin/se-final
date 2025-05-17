package fin.phoenix.flix.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fin.phoenix.flix.MainActivity
import fin.phoenix.flix.R
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.MessageTypes

/**
 * 通知帮助类
 * 处理应用中所有的通知创建和权限检查逻辑
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_SERVICE = "message_service"
        const val CHANNEL_ID_MESSAGES = "messages"
        const val CHANNEL_ID_SYSTEM = "system"

        const val NOTIFICATION_ID_SERVICE = 1
    }

    /**
     * 创建所有需要的通知渠道
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 服务通知渠道 - 低优先级，静音
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "消息服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持消息服务运行"
                setShowBadge(false)
            }

            // 消息通知渠道 - 默认优先级，有声音
            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "消息通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "新消息提醒"
                setShowBadge(true)
            }

            // 系统通知渠道 - 高优先级
            val systemChannel = NotificationChannel(
                CHANNEL_ID_SYSTEM,
                "系统通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "重要的系统通知"
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannels(
                listOf(serviceChannel, messageChannel, systemChannel)
            )
        }
    }

    /**
     * 创建服务通知
     */
    fun createServiceNotification(content: String = "消息服务运行中") = 
        NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setContentTitle("Flix")
            .setContentText(content)
            .setSmallIcon(R.drawable.icons8_notification)
            .setContentIntent(createMainActivityPendingIntent())
            .build()

    /**
     * 发送消息通知
     */
    fun showMessageNotification(message: Message) {
        // 检查通知权限
        if (!hasNotificationPermission()) {
            return
        }

        // 确定通知渠道
        val channelId = when(message.messageType) {
            MessageTypes.NOTIFICATION,
            MessageTypes.SYSTEM -> CHANNEL_ID_SYSTEM
            else -> CHANNEL_ID_MESSAGES
        }
        
        val intent = createIntentForMessage(message)
        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(when(message.messageType) {
                MessageTypes.CHAT -> "新消息"
                MessageTypes.NOTIFICATION -> "系统通知"
                MessageTypes.SYSTEM -> "系统公告"
                else -> "新消息"
            })
            .setContentText(getMessageText(message))
            .setSmallIcon(R.drawable.icons8_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(message.id.hashCode(), notification)
            }
        } catch (e: SecurityException) {
            // 记录权限错误但不抛出异常
        }
    }

    /**
     * 更新服务通知
     */
    fun updateServiceNotification(content: String) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val notification = createServiceNotification(content)
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_SERVICE, notification)
            }
        } catch (e: SecurityException) {
            // 记录权限错误但不抛出异常
        }
    }

    /**
     * 检查通知权限
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // 在API级别33之前不需要特别检查
        }
    }

    /**
     * 创建到MainActivity的PendingIntent
     */
    private fun createMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 基于消息创建适当的Intent
     */
    private fun createIntentForMessage(message: Message): Intent {
        return when(message.messageType) {
            MessageTypes.CHAT -> Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_CHAT
                putExtra("conversation_id", message.conversationId)
            }
            MessageTypes.NOTIFICATION -> Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_NOTIFICATIONS
            }
            MessageTypes.SYSTEM -> Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_ANNOUNCEMENTS
            }
            else -> Intent(context, MainActivity::class.java)
        }
    }

    /**
     * 从消息中提取文本内容
     */
    private fun getMessageText(message: Message): String {
        val firstContent = message.content.firstOrNull()
        return when {
            firstContent?.type == "text" && firstContent.payload is String -> 
                firstContent.payload.toString()
            firstContent?.payload != null -> {
                try {
                    firstContent.payload.toString()
                } catch (e: Exception) {
                    "新消息"
                }
            }
            else -> "新消息"
        }
    }
}