package top.kagg886.pmf

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.ui.component.scroll.LocalScrollbarStyle
import top.kagg886.pmf.ui.component.scroll.defaultScrollbarStyle

private val lightScrollbarStyle = defaultScrollbarStyle()
private val darkScrollbarStyle = defaultScrollbarStyle().copy(
    unhoverColor = Color.White.copy(alpha = 0.50f),
    hoverColor = Color.White.copy(alpha = 0.35f),
)

@Composable
fun PixivMultiPlatformTheme(
    darkModeValue: AppConfig.DarkMode = AppConfig.DarkMode.System,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalScrollbarStyle provides when (darkModeValue) {
            AppConfig.DarkMode.System -> if (isSystemInDarkTheme()) darkScrollbarStyle else lightScrollbarStyle
            AppConfig.DarkMode.Light -> lightScrollbarStyle
            AppConfig.DarkMode.Dark -> darkScrollbarStyle
        },
    ) {
        MaterialTheme(
            colorScheme = when (darkModeValue) {
                AppConfig.DarkMode.System -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                AppConfig.DarkMode.Light -> lightColorScheme()
                AppConfig.DarkMode.Dark -> darkColorScheme()
            },
            content = content
        )
    }
}