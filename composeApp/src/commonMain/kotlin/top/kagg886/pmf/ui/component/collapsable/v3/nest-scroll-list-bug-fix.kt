package top.kagg886.pmf.ui.component.collapsable.v3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import kotlinx.coroutines.launch

fun Modifier.nestedScrollWorkaround(
    scrollableState: ScrollableState,
    connectedScrollState: ConnectedScrollState,
): Modifier {
    return composed {
        val scope = rememberCoroutineScope()
        var isInProgress = false
        onPointerEventMultiplatform(PointerEventType.Scroll, pass = PointerEventPass.Final) {
            if (isInProgress) return@onPointerEventMultiplatform

            val event = it.changes.getOrNull(0) ?: return@onPointerEventMultiplatform
            if (event.type != PointerType.Mouse) {
                // 只有鼠标有 bug
                return@onPointerEventMultiplatform
            }

            val scrollDelta = event.scrollDelta

            if (scrollDelta != Offset.Unspecified && scrollDelta != Offset.Zero) {
                if (!scrollableState.canScrollBackward && scrollDelta.y < -0.5f) { // 0.5 为阈值, 防止稍微动一下
//                    connectedScrollState.scrollableState.dispatchRawDelta(-scrollDelta.y) // 太慢了

                    isInProgress = true
                    scope.launch {
                        try {
                            // 直接滑到顶部
                            connectedScrollState.scrollableState.animateScrollBy(
                                -connectedScrollState.scrolledOffset,
                                tween(500, easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1f)),
                            )
                        } finally {
                            isInProgress = false
                        }
                    }
                }
            }
        }
    }
}
