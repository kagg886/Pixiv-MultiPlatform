package top.kagg886.pmf.ui.component.collapsable.v3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

val LocalConnectedStateKey = compositionLocalOf<ConnectedScrollState?> {
    null
}

data class CollapseTopAppBarScaffoldScope(
    val connectedScrollState: ConnectedScrollState,
) {
    fun Modifier.fixComposeListScrollToTopBug(state: ScrollableState) = this
        .nestedScrollWorkaround(state, connectedScrollState)
}

/**
 * 支持滑动隐藏 Toolbar 的 Scaffold
 * @param background 背景
 * @param title 标题 仅在收折状态下显示
 * @param navigationIcon 导航图标 任何情况下都会显示
 * @param actions 操作按钮 任何情况下都会显示
 * @param content 内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsableTopAppBarScaffold(
    modifier: Modifier = Modifier,
    background: @Composable (Modifier) -> Unit,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable CollapseTopAppBarScaffoldScope.(Modifier) -> Unit,
) {
    val topAppBarHeight = with(LocalDensity.current) {
        TopAppBarDefaults.MediumAppBarCollapsedHeight.toPx().roundToInt()
    }
    val connectedScrollState = rememberConnectedScrollState()
    val scope = remember(connectedScrollState) {
        CollapseTopAppBarScaffoldScope(connectedScrollState)
    }

    Box(modifier) {
        background(
            // see connectedScrollTarget(connectedScrollState)
            Modifier.connectedScrollContainer(connectedScrollState).onSizeChanged {
                connectedScrollState.containerHeight = it.height - topAppBarHeight
            },
        )

        content(
            scope,
            Modifier.fillMaxSize()
                .padding(
                    top = with(LocalDensity.current) {
                        (connectedScrollState.containerHeight - connectedScrollState.scrolledOffset.absoluteValue + topAppBarHeight).toDp()
                    },
                )
                .nestedScroll(connectedScrollState.nestedScrollConnection),
        )

        // 透明标题栏，无论如何都会显示
        TopAppBar(
            title = {},
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        // 只有滑到顶才会出现的标题栏
        AnimatedVisibility(
            visible = connectedScrollState.isScrolledTop,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        }
    }
}
