package top.kagg886.pmf.ui.route.main.search.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import top.kagg886.pmf.ui.route.main.search.v2.components.HistoryItem
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

class EmptySearchScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = rememberScreenModel { EmptySearchViewModel() }
        val state by model.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }

        model.collectSideEffect { sideEffect ->
            when (sideEffect) {
                is EmptySearchSideEffect.Toast -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
            }
        }

        val histories by state.historyFlow.collectAsState(listOf())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("搜索")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navigator.pop()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        navigator.push(SearchPanelScreen())
                    }
                ) {
                    Icon(Icons.Default.Search, null)
                }
            }
        ) { paddingValues ->
            if (histories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无历史记录")
                        Text("点击搜索按钮进行一次搜索吧！")
                    }
                }
            } else {
                LazyColumn(Modifier.padding(paddingValues)) {
                    items(histories) { item ->
                        HistoryItem(
                            onHistoryDelete = {
                                model.deleteHistory(item)
                            },
                            onHistoryClicked = {
                                navigator.push(
                                    SearchPanelScreen(
                                        sort = item.initialSort,
                                        target = item.initialTarget,
                                        keyword = item.keyword
                                    )
                                )
                            },
                            item = item
                        )
                    }
                }
            }
        }
    }
}
