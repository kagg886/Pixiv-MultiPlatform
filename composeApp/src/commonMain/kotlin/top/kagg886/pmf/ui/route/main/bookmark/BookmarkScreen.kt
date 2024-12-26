package top.kagg886.pmf.ui.route.main.bookmark

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.user.FavoriteTagsType
import top.kagg886.pixko.module.user.FavoriteTagsType.*
import top.kagg886.pixko.module.user.TagFilter
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pmf.*
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.book
import top.kagg886.pmf.illust
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.SupportRTLModalNavigationDrawer
import top.kagg886.pmf.ui.component.painterResource
import top.kagg886.pmf.ui.route.main.profile.ProfileScreen
import top.kagg886.pmf.ui.util.*
import top.kagg886.pmf.view

class BookmarkScreen : Screen {
    @Composable
    override fun Content() {
        val model = rememberScreenModel {
            BookmarkViewModel()
        }
        val state by model.collectAsState()
        BookmarkContent(model, state)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BookmarkContent(model: BookmarkViewModel, state: BookmarkViewState) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        when (state) {
            is BookmarkViewState.Loading -> Loading()

            is BookmarkViewState.LoadSuccess -> {
                val tagModel = rememberScreenModel(tag = "favorite_${state.restrict}_${state.mode}") {
                    TagsFetchViewModel(state.restrict, state.mode)
                }
                ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            TagsFetchDrawerSheetContainer(tagModel)
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
                                actions = {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                model.selectMode(
                                                    when (state.mode) {
                                                        Illust -> Novel
                                                        Novel -> Illust
                                                    }
                                                )
                                            }
                                        ) {
                                            AnimatedContent(
                                                state.mode,
                                            ) {
                                                when (it) {
                                                    Illust -> Icon(painterResource(Res.drawable.illust), null)
                                                    Novel -> Icon(painterResource(Res.drawable.book), null)
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                model.selectPublicity(
                                                    when (state.restrict) {
                                                        UserLikePublicity.PUBLIC -> UserLikePublicity.PRIVATE
                                                        UserLikePublicity.PRIVATE -> UserLikePublicity.PUBLIC
                                                    }
                                                )
                                            }
                                        ) {
                                            AnimatedContent(
                                                state.restrict,
                                            ) {
                                                when (it) {
                                                    UserLikePublicity.PUBLIC -> Icon(painterResource(Res.drawable.view), null)
                                                    UserLikePublicity.PRIVATE -> Icon(painterResource(Res.drawable.viewoff), null)
                                                }
                                            }
                                        }
                                    }
                                },
                                navigationIcon = {
                                    Row {
                                        val nav = LocalNavigator.currentOrThrow
                                        IconButton(
                                            onClick = {
                                                //TODO 有bug
                                                nav.popAll()
                                                nav.push(ProfileScreen(PixivConfig.pixiv_user!!))
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = null
                                            )
                                        }

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
                                },
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