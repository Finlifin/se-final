package fin.phoenix.flix.ui.message.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fin.phoenix.flix.data.Message
import fin.phoenix.flix.data.SystemAnnouncementContent
import fin.phoenix.flix.data.SystemNotificationContent
import fin.phoenix.flix.data.TextMessageContent

/**
 * 系统通知/公告消息
 */
@Composable
fun SystemMessageItem(message: Message) {
    val text = when (val content = message.content) {
        is SystemNotificationContent -> content.text
        is SystemAnnouncementContent -> content.text
        is TextMessageContent -> content.text
        else -> return
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFECECEC))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}