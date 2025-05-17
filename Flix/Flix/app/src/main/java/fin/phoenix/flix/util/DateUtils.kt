package fin.phoenix.flix.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ConstantLocale")
private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
@SuppressLint("ConstantLocale")
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
@SuppressLint("ConstantLocale")
private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

fun formatDate(date: Date?): String {
    if (date == null) return ""
    
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }
    
    return when {
        // 今天
        now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
            timeFormat.format(date)
        }
        // 昨天
        now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
            "昨天 ${timeFormat.format(date)}"
        }
        // 今年
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            dateFormat.format(date)
        }
        // 其他年份
        else -> {
            fullDateFormat.format(date)
        }
    }
}

fun formatTime(date: Date?): String {
    return date?.let { timeFormat.format(it) } ?: ""
}