package top.kagg886.pmf.ui.route.main.series.novel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pmf.util.stringResource
import top.kagg886.pixko.module.novel.SeriesDetail
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.novel_count
import top.kagg886.pmf.novel_series
import top.kagg886.pmf.openBrowser
import top.kagg886.pmf.open_in_browser
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.SupportRTLModalNavigationDrawer
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.word_count

class NovelSeriesScreen(private val id: Int) : Screen {
    override val key: ScreenKey = "novel_series_$id"

    @Composable
    override fun Content() {
        val model = rememberScreenModel {
            NovelSeriesScreenModel(id)
        }
        val snack = LocalSnackBarHost.current
        model.collectSideEffect {
            when (it) {
                is NovelSeriesScreenSideEffect.Toast -> {
                    snack.showSnackbar(it.msg)
                }
            }
        }
        val state by model.collectAsState()
        NovelSeriesScreenContent(state, model)
    }

    @Composable
    private fun NovelSeriesScreenContent(state: NovelSeriesScreenState, model: NovelSeriesScreenModel) {
        when (state) {
            NovelSeriesScreenState.Loading -> Loading()
            is NovelSeriesScreenState.LoadingFailed -> ErrorPage(text = state.msg) {
                model.reload()
            }

            is NovelSeriesScreenState.LoadingSuccess -> {
                val novelModel = rememberScreenModel {
                    NovelSeriesFetchModel(id)
                }

                if (useWideScreenMode) {
                    WideNovelSeriesScreenContent(state.info, novelModel)
                    return
                }
                NovelSeriesScreenContent(state.info, novelModel)
            }
        }
    }

    @Composable
    private fun WideNovelSeriesScreenContent(info: SeriesDetail, model: NovelFetchViewModel) {
        PermanentNavigationDrawer(
            drawerContent = {
                NovelSeriesScreenDrawerContent(info)
            },
        ) {
            NovelFetchScreen(model)
        }
    }

    @Composable
    private fun NovelSeriesScreenContent(info: SeriesDetail, model: NovelFetchViewModel) {
        val state = rememberDrawerState(DrawerValue.Open)
        SupportRTLModalNavigationDrawer(
            drawerContent = {
                NovelSeriesScreenDrawerContent(info)
            },
            drawerState = state,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(stringResource(Res.string.novel_series))
                        },
                        navigationIcon = {
                            val scope = rememberCoroutineScope()
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        state.open()
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    null,
                                )
                            }
                        },
                        actions = {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    expanded = true
                                },
                            ) {
                                Icon(Icons.Default.Menu, null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.open_in_browser)) },
                                    onClick = {
                                        openBrowser("https://www.pixiv.net/novel/series/$id")
                                        expanded = false
                                    },
                                )
                            }
                        },
                    )
                },
            ) {
                Box(Modifier.padding(it)) {
                    NovelFetchScreen(model)
                }
            }
        }
    }

    @Composable
    private fun NovelSeriesScreenDrawerContent(info: SeriesDetail) {
        PermanentDrawerSheet {
            Column(Modifier.width(DrawerDefaults.MaximumDrawerWidth)) {
                TopAppBar(
                    title = {
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    navigationIcon = {
                        val nav = LocalNavigator.currentOrThrow
                        IconButton(
                            onClick = {
                                nav.pop()
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                            )
                        }
                    },
                )

                LazyColumn {
                    item {
                        OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            ListItem(
                                headlineContent = {
                                    Text(info.caption)
                                },
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        // TODO 添加关注
                        val model = rememberScreenModel {
                            NovelSeriesScreenModel(id)
                        }
                        AuthorCard(
                            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            info.user,
                            onFavoriteClick = {
                                val job = if (it) model.followUser(false) else model.unFollowUser()
                                job.join()
                            },
                            onFavoritePrivateClick = {
                                val job = model.followUser(true)
                                job.join()
                            },
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            ListItem(
                                overlineContent = {
                                    Text(stringResource(Res.string.novel_count))
                                },
                                headlineContent = {
                                    Text(info.pageCount.toString())
                                },
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            ListItem(
                                overlineContent = {
                                    Text(stringResource(Res.string.word_count))
                                },
                                headlineContent = {
                                    Text(info.totalCharacterCount.toString())
                                },
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
