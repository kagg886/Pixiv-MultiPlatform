package top.kagg886.pmf.ui.component

import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.ui.component.BackToTopState.*

enum class BackToTopState {
    HIDE,
    SHOW_BTT,
    SHOW_RFH
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
            currentPlatform !is Platform.Android -> SHOW_RFH
            else -> HIDE
        }
    }

    AnimatedContent(
        targetState = state0,
        modifier = modifier,
        transitionSpec = {
            slideInVertically { it / 2 } + fadeIn() togetherWith
                    slideOutVertically { it / 2 } + fadeOut()
        }
    ) { state ->
        when (state) {
            HIDE -> {}
            SHOW_BTT -> {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            onBackToTop()
                        }
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, null)
                }
            }

            SHOW_RFH -> {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            onRefresh()
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        }
    }
}