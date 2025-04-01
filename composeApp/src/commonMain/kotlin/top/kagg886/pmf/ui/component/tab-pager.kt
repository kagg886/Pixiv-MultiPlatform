package top.kagg886.pmf.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.zIndex
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch
import top.kagg886.pmf.ui.util.KeyListenerFromGlobalPipe

@Composable
fun TabContainer(
    modifier: Modifier = Modifier,
    state: MutableState<Int>,
    tab: List<String>,
    scrollable: Boolean = false,
    page: @Composable (Int) -> Unit,
) {
    val pageIndex by state
    val pagerState = rememberPagerState(state.value) { tab.size }
    LaunchedEffect(pagerState.currentPage) {
        state.value = pagerState.currentPage
    }
    val scope = rememberCoroutineScope()
    Column(modifier) {
        val content = @Composable {
            for (i in tab.indices) {
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(i)
                        }
                    },
                    text = {
                        Text(text = tab[i])
                    },
                )
            }
        }
        if (scrollable) {
            ScrollableTabRow(
                selectedTabIndex = pageIndex,
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                divider = {},
                tabs = content,
            )
        } else {
            TabRow(
                selectedTabIndex = pageIndex,
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                tabs = content,
            )
        }

        KeyListenerFromGlobalPipe {
            if (it.type != KeyEventType.KeyUp) return@KeyListenerFromGlobalPipe
            when (it.key) {
                Key.DirectionRight -> {
                    pagerState.animateScrollToPage(min(pagerState.currentPage + 1, pagerState.pageCount - 1))
                }
                Key.DirectionLeft -> {
                    pagerState.animateScrollToPage(max(pagerState.currentPage - 1, 0))
                }
            }
        }

        HorizontalPager(
            state = pagerState,
        ) {
            page(it)
        }
    }
}
