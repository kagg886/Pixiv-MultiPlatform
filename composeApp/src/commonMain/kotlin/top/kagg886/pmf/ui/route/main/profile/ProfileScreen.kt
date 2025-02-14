package top.kagg886.pmf.ui.route.main.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.user.SimpleMeProfile
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.route.main.bookmark.BookmarkScreen
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreenWithoutCollapse
import top.kagg886.pmf.ui.route.main.download.DownloadScreen
import top.kagg886.pmf.ui.route.main.history.HistoryScreen
import top.kagg886.pmf.ui.route.main.profile.ProfileItem.*
import top.kagg886.pmf.ui.route.main.setting.SettingScreen
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.util.SerializableWrapper
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

    @OptIn(ExperimentalMaterial3Api::class)
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
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
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
                                    onClick = { nav.pop() }
                                ) {
                                    Icon(Icons.AutoMirrored.Default.ArrowBack, "")
                                }
                            },
                            trailingContent = {
                                ProgressedAsyncImage(
                                    url = me.profileImageUrls.content,
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                        )
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    val scope = rememberCoroutineScope()
                    NavigationDrawerItem(
                        label = {
                            Text("个人信息")
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
                        }
                    )

                    NavigationDrawerItem(
                        label = {
                            Text("我的收藏")
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
                        }
                    )

                    NavigationDrawerItem(
                        label = {
                            Text("下载管理")
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
                        }
                    )

                    NavigationDrawerItem(
                        label = {
                            Text("历史记录")
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
                        }
                    )

                    NavigationDrawerItem(
                        label = {
                            Text("程序设置")
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
                        }
                    )

                }
            },
            content = {
                @Composable
                fun Content() {
                    AnimatedContent(targetState = page) {
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
                                        ViewProfile -> "个人信息"
                                        History -> "历史记录"
                                        Download -> "下载管理"
                                        Setting -> "程序设置"
                                    }
                                )
                            },
                            navigationIcon = {
                                val scope = rememberCoroutineScope()
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawer.open()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Menu, "")
                                }
                            }
                        )
                    }
                ) {
                    Box(Modifier.fillMaxSize().padding(it)) {
                        Content()
                    }
                }
            }
        )
    }


    @Composable
    fun ProfileScreenContainDrawerScaffold(
        state: DrawerState,
        content: @Composable () -> Unit,
        drawerContent: @Composable () -> Unit
    ) {
        if (useWideScreenMode) {
            PermanentNavigationDrawer(
                drawerContent = drawerContent,
                content = content
            )
            return
        }
        ModalNavigationDrawer(
            drawerContent = drawerContent,
            drawerState = state,
            gesturesEnabled = true,
            content = content
        )
    }

}
