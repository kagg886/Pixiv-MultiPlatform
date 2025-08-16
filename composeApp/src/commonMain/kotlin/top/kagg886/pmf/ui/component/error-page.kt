package top.kagg886.pmf.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

@Composable
fun ErrorPage(
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    text: String,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Default.Warning,
    buttonText: String? = null,
) {
    // 添加淡入动画
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "error_page_fade_in",
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = modifier.fillMaxSize().alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        // 主要内容区域 - 使用更简洁的设计
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 错误图标 - 使用更柔和的颜色
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )

            // 错误文本 - 使用更柔和的样式
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            // 重试按钮 - 使用更简洁的样式
            TextButton(
                onClick = onClick,
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buttonText ?: "重试",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        // 返回按钮 - 使用更简洁的样式
        if (showBackButton) {
            val nav = LocalNavigator.currentOrThrow
            IconButton(
                onClick = { nav.pop() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// 向后兼容的重载函数
@Composable
fun ErrorPage(
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    text: String,
    onClick: () -> Unit,
) {
    ErrorPage(
        modifier = modifier,
        showBackButton = showBackButton,
        text = text,
        onClick = onClick,
        icon = Icons.Default.Warning,
        buttonText = null,
    )
}
