package fin.phoenix.flix.ui.theme

import android.app.Activity
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import fin.phoenix.flix.ui.colors.DarkRoseRed
import fin.phoenix.flix.ui.colors.LightRoseRed
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.ui.colors.WarnRoseRed
import fin.phoenix.flix.util.PreferencesManager

private val LightColorScheme = lightColorScheme(
    primary = RoseRed, 
    primaryContainer = LightRoseRed, 
    secondary = DarkRoseRed, 
    error = WarnRoseRed,
    surface = Color.White,
    background = Color(0xFFF8F8F8)
)

private val DarkColorScheme = darkColorScheme(
    primary = RoseRed, 
    primaryContainer = DarkRoseRed, 
    secondary = LightRoseRed, 
    error = WarnRoseRed,
    surface = Color.White,
    background = Color(0xFF121212)
)

// 自定义Card颜色提供者
//val LocalCardColors = staticCompositionLocalOf {
//    CardDefaults.cardColors(containerColor = Color.White)
//}

@Composable
fun FlixTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val darkTheme = remember { mutableStateOf(preferencesManager.isDarkMode) }

    val colorScheme = if (darkTheme.value) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Set system bars color and appearance
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.navigationBarColor = colorScheme.surface.toArgb()
            window.statusBarColor = colorScheme.surface.toArgb()

            // Set status bar content color based on theme
            insetsController.isAppearanceLightStatusBars = !darkTheme.value
            insetsController.isAppearanceLightNavigationBars = !darkTheme.value
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, 
        content = content
    )
}