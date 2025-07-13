package top.kagg886.pmf.ui.route.main.bookmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.pixko.module.user.FavoriteTagsType
import top.kagg886.pixko.module.user.FavoriteTagsType.Illust
import top.kagg886.pixko.module.user.FavoriteTagsType.Novel
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pmf.Res
import top.kagg886.pmf.bookmark
import top.kagg886.pmf.bookmark_settings
import top.kagg886.pmf.illust
import top.kagg886.pmf.novel
import top.kagg886.pmf.private
import top.kagg886.pmf.public
import top.kagg886.pmf.type
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.SupportRTLModalNavigationDrawer
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.TagsFetchDrawerSheetContainer
import top.kagg886.pmf.ui.util.TagsFetchViewModel
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.visibility

class BookmarkScreen : Screen {
    @Composable
    override fun Content() {
        val model = rememberScreenModel {
            BookmarkViewModel()
        }
        val nav = LocalNavigator.currentOrThrow
        val state by model.collectAsState()
        BookmarkContent(model, state) {
            nav.pop()
        }
    }

    @OptIn(InternalVoyagerApi::class)
    @Composable
    private fun BookmarkContent(model: BookmarkViewModel, state: BookmarkViewState, goBack: () -> Unit) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        when (state) {
            is BookmarkViewState.Loading -> Loading()

            is BookmarkViewState.LoadSuccess -> {
                val tagModel = rememberScreenModel(tag = "favorite_${state.restrict}_${state.mode}") {
                    TagsFetchViewModel(state.restrict, state.mode)
                }

                BackHandler(drawerState.isOpen) {
                    scope.launch {
                        drawerState.close()
                    }
                }

                SupportRTLModalNavigationDrawer(
                    rtlLayout = true,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(Modifier.width(DrawerDefaults.MaximumDrawerWidth)) {
                                TopAppBar(
                                    title = {
                                        Text(stringResource(Res.string.bookmark_settings))
                                    },
                                )
                                ListItem(
                                    overlineContent = {
                                        Text(stringResource(Res.string.visibility))
                                    },
                                    headlineContent = {
                                        TabRow(
                                            selectedTabIndex = state.restrict.ordinal,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth(),
                                            divider = {},
                                            tabs = {
                                                for (entry in UserLikePublicity.entries) {
                                                    Tab(
                                                        selected = state.restrict == entry,
                                                        onClick = {
                                                            model.selectPublicity(entry)
                                                        },
                                                        text = {
                                                            when (entry) {
                                                                UserLikePublicity.PUBLIC -> Text(stringResource(Res.string.public))
                                                                UserLikePublicity.PRIVATE -> Text(stringResource(Res.string.private))
                                                            }
                                                        },
                                                    )
                                                }
                                            },
                                        )
                                    },
                                )
                                ListItem(
                                    overlineContent = {
                                        Text(stringResource(Res.string.type))
                                    },
                                    headlineContent = {
                                        TabRow(
                                            selectedTabIndex = state.mode.ordinal,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth(),
                                            divider = {},
                                            tabs = {
                                                for (entry in FavoriteTagsType.entries) {
                                                    Tab(
                                                        selected = state.mode == entry,
                                                        onClick = {
                                                            model.selectMode(entry)
                                                        },
                                                        text = {
                                                            when (entry) {
                                                                Illust -> Text(stringResource(Res.string.illust))
                                                                Novel -> Text(stringResource(Res.string.novel))
                                                            }
                                                        },
                                                    )
                                                }
                                            },
                                        )
                                    },
                                )
                                TagsFetchDrawerSheetContainer(tagModel)
                            }
                        }
                    },
                    drawerState = drawerState,
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(stringResource(Res.string.bookmark))
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            goBack()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        },
                    ) {
                        Box(Modifier.padding(it).fillMaxSize()) {
                            val tagState by tagModel.collectAsState()
                            LaunchedEffect(tagState) {
                                drawerState.close()
                                model.selectTagFilter(tagState.selectedTagsFilter)
                            }

                            when (state.mode) {
                                Illust -> {
                                    val illustModel =
                                        rememberScreenModel(tag = "favorite_${state.restrict}_${state.tagFilter}") {
                                            BookmarkIllustViewModel(state.restrict, state.tagFilter)
                                        }
                                    IllustFetchScreen(illustModel)
                                }

                                Novel -> {
                                    val novelModel =
                                        rememberScreenModel(tag = "favorite_${state.restrict}_${state.tagFilter}") {
                                            BookmarkNovelViewModel(state.restrict, state.tagFilter)
                                        }
                                    NovelFetchScreen(novelModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
