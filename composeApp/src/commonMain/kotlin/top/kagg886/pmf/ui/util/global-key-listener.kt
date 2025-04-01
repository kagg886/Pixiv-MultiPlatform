package top.kagg886.pmf.ui.util

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch
import top.kagg886.pmf.LocalKeyStateFlow

@Composable
fun KeyListenerFromGlobalPipe(block: suspend (KeyEvent) -> Unit) {
    val flow = LocalKeyStateFlow.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        flow.collect {
            scope.launch {
                block(it)
            }
        }
    }
}

fun keyboardScrollerController(
    scrollableState: ScrollableState,
    viewPortHeightFunc: () -> Float,
): suspend (KeyEvent) -> Unit {
    return block@{
        val viewPortHeight = viewPortHeightFunc()
        if (it.type != KeyEventType.KeyDown) return@block
        when (it.key) {
            Key.DirectionDown -> {
                scrollableState.animateScrollBy(viewPortHeight * 0.4f)
            }

            Key.DirectionUp -> {
                scrollableState.animateScrollBy(viewPortHeight * -0.4f)
            }

            Key.Spacebar, Key.PageDown -> {
                scrollableState.animateScrollBy(viewPortHeight.toFloat())
            }

            Key.PageUp -> {
                scrollableState.animateScrollBy(-viewPortHeight.toFloat())
            }
        }
    }
}
