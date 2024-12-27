package top.kagg886.pmf.ui.route.main.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
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
import top.kagg886.pixko.module.user.FavoriteTagsType
import top.kagg886.pixko.module.user.FavoriteTagsType.Illust
import top.kagg886.pixko.module.user.FavoriteTagsType.Novel
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.SupportRTLModalNavigationDrawer
import top.kagg886.pmf.ui.route.main.profile.ProfileScreen
import top.kagg886.pmf.ui.util.*

class BookmarkScreen : Screen {
    @Composable
    override fun Content() {
        val model = rememberScreenModel {
            BookmarkViewModel()
        }
        val nav = LocalNavigator.currentOrThrow
        val state by model.collectAsState()
        BookmarkContent(model, state) {
            //TODO 有bug
            nav.popAll()
            nav.push(ProfileScreen(PixivConfig.pixiv_user!!))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, InternalVoyagerApi::class)
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
                                        Text("收藏设置")
                                    },
                                )
                                ListItem(
                                    overlineContent = {
                                        Text("可见性")
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
                                                                UserLikePublicity.PUBLIC -> Text("公开")
                                                                UserLikePublicity.PRIVATE -> Text("私密")
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    },
                                )
                                ListItem(
                                    overlineContent = {
                                        Text("类型")
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
                                                                Illust -> Text("插画")
                                                                Novel -> Text("小说")
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }
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
                                    Text("收藏")
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = {
                                            goBack()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    ) {
                        Box(Modifier.padding(it).fillMaxSize()) {
                            val tagState by tagModel.collectAsState()
                            LaunchedEffect(tagState) {
                                (tagState as? TagsFetchViewState.ShowTagsList)?.let {
                                    drawerState.close()
                                    model.selectTagFilter(it.selectedTagsFilter)
                                }
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