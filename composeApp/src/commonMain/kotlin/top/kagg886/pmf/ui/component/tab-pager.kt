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
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

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
                    }
                )
            }
        }
        if (scrollable) {
            ScrollableTabRow(
                selectedTabIndex = pageIndex,
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                divider = {},
                tabs = content
            )
        } else {
            TabRow(
                selectedTabIndex = pageIndex,
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                tabs = content
            )
        }

        HorizontalPager(
            state = pagerState
        ) {
            page(it)
        }
    }
}