package top.kagg886.pmf.ui.component.collapsable.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 支持滑动隐藏 Toolbar 的 Scaffold
 * @param toolbarSize Toolbar 高度
 * @param toolbar  Toolbar 可组合函数
 * @param content 主体，需要联动滑动的需要指定 NestedScrollConnection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsableTopAppBarScaffold(
    modifier: Modifier = Modifier,
    toolbarSize: Dp = TopAppBarDefaults.MediumAppBarCollapsedHeight,
    toolbar: @Composable () -> Unit,
    smallToolBar: (@Composable () -> Unit)? = null,
    content: @Composable (NestedScrollConnection) -> Unit
) {
    val density = LocalDensity.current

    val toolbarHeight = remember(toolbarSize) {
        with(density) { toolbarSize.toPx() }
    }

    // TopAppbar 的 offset
    val offset = remember {
        mutableStateOf(0f)
    }
    val nestedScrollConn = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // 滑动计算，消费让 TopAppbar 滑动的对应 Offset，同时让 TopAppbar 滑动
            var off = (offset.value) + available.y
            var con = available.y
            if (off >= 0) {
                off = 0f
                con = 0 - (offset.value)
            }
            if (off <= -(toolbarHeight)) {
                off = -(toolbarHeight)
                con = -(toolbarHeight) - (offset.value)
            }
            offset.value = off
            return Offset(0f, con)
        }
    }
    // 为了避免遮挡，这里需要分两层
    Box(modifier) {
        // 主体层使用一个 Spacer 占位
        val height0 = remember(toolbarHeight, offset.value) {
            with(density) {
                ((toolbarHeight) - (-offset.value)).toDp()
            }
        }
        Column {
            Spacer(modifier = Modifier.height(height0))
            AnimatedVisibility(
                visible = height0 <= 0.dp,
                exit = fadeOut(animationSpec = tween(1))
            ) { smallToolBar?.let { it() } }
            content(nestedScrollConn)
        }
        // TopAppbar 层随着 offset 移动
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(toolbarSize)
                .graphicsLayer { translationY = (offset.value) }) {
            toolbar()
        }
    }
}