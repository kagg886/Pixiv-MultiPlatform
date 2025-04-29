package top.kagg886.pmf.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.ui.component.BackToTopState.*
import top.kagg886.pmf.ui.util.KeyListenerFromGlobalPipe

enum class BackToTopState {
    HIDE,
    SHOW_BTT,
    SHOW_RFH,
}

@Composable
fun BackToTopOrRefreshButton(
    isNotInTop: Boolean,
    modifier: Modifier = Modifier,
    onBackToTop: suspend () -> Unit = {},
    onRefresh: suspend () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val state0 = remember(isNotInTop) {
        when {
            isNotInTop -> SHOW_BTT
            currentPlatform is Platform.Desktop -> SHOW_RFH
            else -> HIDE
        }
    }

    AnimatedContent(
        targetState = state0,
        modifier = modifier,
        transitionSpec = {
            slideInVertically { it / 2 } + fadeIn() togetherWith
                slideOutVertically { it / 2 } + fadeOut()
        },
    ) { state ->
        when (state) {
            HIDE -> {
                // Android use this state
                KeyListenerFromGlobalPipe {
                    if (it.type != KeyEventType.KeyUp) return@KeyListenerFromGlobalPipe
                    if (it.key == Key.R) {
                        onRefresh()
                    }
                }
                Spacer(Modifier.size(88.dp, 88.dp)) // placeholder
            }
            SHOW_BTT -> {
                KeyListenerFromGlobalPipe {
                    if (it.type != KeyEventType.KeyUp) return@KeyListenerFromGlobalPipe
                    if (it.key == Key.R) {
                        onBackToTop()
                    }
                }
                FloatingActionButton(
                    onClick = { scope.launch { onBackToTop() } },
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, null)
                }
            }

            SHOW_RFH -> {
                KeyListenerFromGlobalPipe {
                    if (it.type != KeyEventType.KeyUp) return@KeyListenerFromGlobalPipe
                    if (it.key == Key.R) {
                        onRefresh()
                    }
                }
                FloatingActionButton(
                    onClick = { scope.launch { onRefresh() } },
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        }
    }
}
