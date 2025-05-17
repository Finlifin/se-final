package fin.phoenix.flix.ui.login

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fin.phoenix.flix.ui.colors.RoseRed

@Composable
fun TabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (isSelected) RoseRed else Color.Gray,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(8.dp)
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(
                        color = RoseRed, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) RoseRed else Color.LightGray, label = "borderColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isFocused || value.isNotEmpty()) RoseRed else Color.Gray,
        label = "iconColor"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusEvent { x: FocusState -> isFocused = x.isFocused },
        label = { Text(label, color = if (isFocused) RoseRed else Color.Gray) },
        leadingIcon = leadingIcon?.let {
            { CompositionLocalProvider(LocalContentColor provides iconColor) { it() } }
        },
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            cursorColor = RoseRed
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

data class BubbleState(
    val x: Float, val y: Float, val radius: Float, val alpha: Float, val speed: Float
)

fun validateInputs(
    context: android.content.Context,
    phoneNumber: String,
    password: String,
    verificationCode: String,
    isPasswordLogin: Boolean
): Boolean {
    // Validate phone number
    if (phoneNumber.isEmpty()) {
        Toast.makeText(context, "请输入手机号", Toast.LENGTH_SHORT).show()
        return false
    }

    if (phoneNumber.length != 11 || !phoneNumber.all { it.isDigit() }) {
        Toast.makeText(context, "手机号格式不正确", Toast.LENGTH_SHORT).show()
        return false
    }

    if (isPasswordLogin) {
        // Validate password
        if (password.isEmpty()) {
            Toast.makeText(context, "请输入密码", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(context, "密码长度不能少于6位", Toast.LENGTH_SHORT).show()
            return false
        }
    } else {
        // Validate verification code
        if (verificationCode.isEmpty()) {
            Toast.makeText(context, "请输入验证码", Toast.LENGTH_SHORT).show()
            return false
        }

        if (verificationCode.length != 6 || !verificationCode.all { it.isDigit() }) {
            Toast.makeText(context, "验证码格式不正确", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    return true
}
