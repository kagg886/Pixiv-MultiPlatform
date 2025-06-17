package top.kagg886.pmf.ui.route.main.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.user.SimpleMeProfile
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.download_manager
import top.kagg886.pmf.history
import top.kagg886.pmf.my_bookmark
import top.kagg886.pmf.personal_profile
import top.kagg886.pmf.settings
import top.kagg886.pmf.ui.route.main.bookmark.BookmarkScreen
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreenWithoutCollapse
import top.kagg886.pmf.ui.route.main.download.DownloadScreen
import top.kagg886.pmf.ui.route.main.history.HistoryScreen
import top.kagg886.pmf.ui.route.main.profile.ProfileItem.Download
import top.kagg886.pmf.ui.route.main.profile.ProfileItem.History
import top.kagg886.pmf.ui.route.main.profile.ProfileItem.Setting
import top.kagg886.pmf.ui.route.main.profile.ProfileItem.ViewProfile
import top.kagg886.pmf.ui.route.main.setting.SettingScreen
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.util.SerializableWrapper
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.util.wrap

enum class ProfileItem {
    ViewProfile,
    History,
    Download,
    Setting,
}

class ProfileScreen(me: SerializableWrapper<SimpleMeProfile>, private val target: ProfileItem) : Screen {
    constructor(me: SimpleMeProfile, target: ProfileItem = ViewProfile) : this(wrap(me), target)

    private val me by me

    @Composable
    override fun Content() {
        var page by rememberSaveable {
            mutableStateOf(target)
        }
        val drawer = rememberDrawerState(DrawerValue.Open)
        ProfileScreenContainDrawerScaffold(
            state = drawer,
            drawerContent = {
                ModalDrawerSheet {
                    val nav = LocalNavigator.currentOrThrow
                    OutlinedCard(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(me.name)
                            },
                            supportingContent = {
                                Text(me.pixivId)
                            },
                            leadingContent = {
                                IconButton(
                                    onClick = { nav.pop() },
                                ) {
                                    Icon(Icons.AutoMirrored.Default.ArrowBack, "")
                                }
                            },
                            trailingContent = {
                                AsyncImage(
                                    model = me.profileImageUrls.content,
                                    modifier = Modifier.size(35.dp).clip(CircleShape),
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    val scope = rememberCoroutineScope()
                    NavigationDrawerItem(
                        label = {
                            Text(stringResource(Res.string.personal_profile))
                        },
                        icon = {
                            Icon(Icons.Default.Person, "")
                        },
                        selected = page == ViewProfile,
                        onClick = {
                            page = ViewProfile
                            scope.launch {
                                drawer.close()
                            }
                        },
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(stringResource(Res.string.my_bookmark))
                        },
                        icon = {
                            Icon(Icons.Default.Favorite, "")
                        },
                        selected = false,
                        onClick = {
                            nav.push(BookmarkScreen())
                            scope.launch {
                                drawer.close()
                            }
                        },
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(stringResource(Res.string.download_manager))
                        },
                        icon = {
                            Icon(top.kagg886.pmf.ui.component.icon.Download, "")
                        },
                        selected = page == Download,
                        onClick = {
                            page = Download
                            scope.launch {
                                drawer.close()
                            }
                        },
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(stringResource(Res.string.history))
                        },
                        icon = {
                            Icon(Icons.Default.MailOutline, "")
                        },
                        selected = page == History,
                        onClick = {
                            page = History
                            scope.launch {
                                drawer.close()
                            }
                        },
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(stringResource(Res.string.settings))
                        },
                        icon = {
                            Icon(Icons.Default.Settings, "")
                        },
                        selected = page == Setting,
                        onClick = {
                            page = Setting
                            scope.launch {
                                drawer.close()
                            }
                        },
                    )
                }
            },
            content = {
                @Composable
                fun Content() {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) togetherWith slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start)
                        },
                    ) {
                        when (it) {
                            ViewProfile -> {
                                AuthorScreenWithoutCollapse(PixivConfig.pixiv_user!!.userId).Content()
                            }

                            History -> {
                                HistoryScreen().Content()
                            }

                            Download -> {
                                DownloadScreen().Content()
                            }

                            Setting -> {
                                SettingScreen().Content()
                            }
                        }
                    }
                }

                if (useWideScreenMode) {
                    Content()
                    return@ProfileScreenContainDrawerScaffold
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when (page) {
                                        ViewProfile -> stringResource(Res.string.personal_profile)
                                        History -> stringResource(Res.string.history)
                                        Download -> stringResource(Res.string.download_manager)
                                        Setting -> stringResource(Res.string.settings)
                                    },
                                )
                            },
                            navigationIcon = {
                                val scope = rememberCoroutineScope()
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawer.open()
                                        }
                                    },
                                ) {
                                    Icon(Icons.Default.Menu, "")
                                }
                            },
                        )
                    },
                ) {
                    Box(Modifier.fillMaxSize().padding(it)) {
                        Content()
                    }
                }
            },
        )
    }

    @Composable
    fun ProfileScreenContainDrawerScaffold(
        state: DrawerState,
        content: @Composable () -> Unit,
        drawerContent: @Composable () -> Unit,
    ) {
        if (useWideScreenMode) {
            PermanentNavigationDrawer(
                drawerContent = drawerContent,
                content = content,
            )
            return
        }
        ModalNavigationDrawer(
            drawerContent = drawerContent,
            drawerState = state,
            gesturesEnabled = true,
            content = content,
        )
    }
}
