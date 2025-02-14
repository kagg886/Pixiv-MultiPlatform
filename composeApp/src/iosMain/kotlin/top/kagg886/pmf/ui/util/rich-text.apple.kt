package top.kagg886.pmf.ui.util

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
actual fun HTMLRichText(
    html: String,
    modifier: Modifier,
    color: Color,
    style: TextStyle
) {
    Text(html, color = color, style = style, modifier = modifier)
}
