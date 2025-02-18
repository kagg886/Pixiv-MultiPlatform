package top.kagg886.pmf.ui.route.main.series.novel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.novel.SeriesDetail
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.openBrowser

import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.SupportRTLModalNavigationDrawer
import top.kagg886.pmf.ui.util.*

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
            }
        ) {
            NovelFetchScreen(model)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
                            Text("小说系列")
                        },
                        navigationIcon = {
                            val scope = rememberCoroutineScope()
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        state.open()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    null
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
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("在浏览器中打开") },
                                    onClick = {
                                        openBrowser("https://www.pixiv.net/novel/series/${id}")
                                        expanded = false
                                    }
                                )
                            }
                        }
                    )
                }
            ) {
                Box(Modifier.padding(it)) {
                    NovelFetchScreen(model)
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NovelSeriesScreenDrawerContent(info: SeriesDetail) {
        PermanentDrawerSheet {
            Column(Modifier.width(DrawerDefaults.MaximumDrawerWidth)) {
                TopAppBar(
                    title = {
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        val nav = LocalNavigator.currentOrThrow
                        IconButton(
                            onClick = {
                                nav.pop()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null
                            )
                        }
                    }
                )

                LazyColumn {
                    item {
                        OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            ListItem(
                                headlineContent = {
                                    Text(info.caption)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        //TODO 添加关注
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
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            ListItem(
                                overlineContent = {
                                    Text("小说数目")
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
                                    Text("总字数")
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
