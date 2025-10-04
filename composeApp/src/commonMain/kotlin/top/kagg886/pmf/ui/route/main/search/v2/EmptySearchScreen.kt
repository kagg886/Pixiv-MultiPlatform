package top.kagg886.pmf.ui.route.main.search.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.route.main.search.v2.components.HistoryItem
import top.kagg886.pmf.util.stringResource

class EmptySearchScreen : Screen {
    @Composable
    override fun Content() {
        val model = rememberScreenModel { EmptySearchViewModel() }
        val state by model.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = LocalSnackBarHost.current

        model.collectSideEffect { sideEffect ->
            when (sideEffect) {
                is EmptySearchSideEffect.Toast -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(Res.string.search))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navigator.pop()
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                model.clearHistory()
                            },
                        ) {
                            Icon(Icons.Default.Delete, null)
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        navigator.push(SearchPanelScreen())
                    },
                ) {
                    Icon(Icons.Default.Search, null)
                }
            },
        ) { paddingValues ->
            when (state) {
                is EmptySearchState.Loading -> Loading()

                is EmptySearchState.ShowHistoryList -> {
                    val historyState = state as EmptySearchState.ShowHistoryList
                    val histories by historyState.historyFlow.collectAsState(listOf())

                    if (histories.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(Res.string.no_history))
                                Text(stringResource(Res.string.click_to_search))
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
                                                keyword = item.keyword,
                                            ),
                                        )
                                    },
                                    item = item,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
