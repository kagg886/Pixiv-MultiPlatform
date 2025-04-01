package top.kagg886.pmf

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import top.kagg886.pmf.ui.component.scroll.LocalScrollbarStyle
import top.kagg886.pmf.ui.component.scroll.defaultScrollbarStyle

@Composable
fun PixivMultiPlatformTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides defaultScrollbarStyle().copy(
                unhoverColor = MaterialTheme.colorScheme.surfaceVariant,
                hoverColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            content,
        )
    }
}
