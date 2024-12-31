package top.kagg886.pmf.ui.route.main.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.user.SimpleMeProfile
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.route.login.LoginScreen
import top.kagg886.pmf.ui.route.main.bookmark.BookmarkScreen
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.route.main.download.DownloadScreen
import top.kagg886.pmf.ui.route.main.history.HistoryScreen
import top.kagg886.pmf.ui.route.main.setting.SettingScreen
import top.kagg886.pmf.util.SerializableWrapper
import top.kagg886.pmf.util.wrap

class ProfileScreen(me: SerializableWrapper<SimpleMeProfile>, private val target: ProfileItem) : Screen {
    override val key: ScreenKey = uniqueScreenKey
    private val me by me

    constructor(me: SimpleMeProfile, target: ProfileItem = ProfileItem.ViewProfile) : this(wrap(me), target)

    private class PageScreenModel : ScreenModel {
        val page: MutableState<Int> = mutableIntStateOf(0)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val page = rememberScreenModel("initial_${target.name}") {
            PageScreenModel().apply {
                page.value = target.ordinal
            }
        }
        var select by page.page
        if (currentPlatform.useWideScreenMode) {
            PermanentNavigationDrawer(
                drawerContent = {
                    SettingDrawerSheet(me = me, select) {
                        select = it
                    }
                },
            ) {
                SettingDrawerContent(me = me, select = select)
            }
            return
        }
        val state = rememberDrawerState(DrawerValue.Open)
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(
            drawerContent = {
                SettingDrawerSheet(me = me, select) {
                    select = it
                    scope.launch {
                        state.close()
                    }
                }
            },
            drawerState = state,
            gesturesEnabled = true,
        ) {
            Scaffold(topBar = {
                TopAppBar(title = {
                    Text(me.name)
                }, navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            state.open()
                        }
                    }) {
                        Icon(Icons.Default.Menu, "")
                    }
                })
            }) {
                Box(modifier = Modifier.fillMaxSize().padding(it)) {
                    SettingDrawerContent(me, select)
                }
            }
        }
    }
}

@Composable
private fun SettingDrawerContent(me: SimpleMeProfile, select: Int) {
    AnimatedContent(targetState = select) {
        ProfileItem.entries[it].content(me)
    }
}

enum class ProfileItem(
    val title: String,
    val icon: ImageVector,
    val content: @Composable (me: SimpleMeProfile) -> Unit
) {
    ViewProfile(
        title = "查看资料",
        icon = Icons.Default.Person,
        content = {
            AuthorScreen(it.userId, true).Content()
        }
    ),
    ExtendsFavorite(
        title = "查看收藏",
        icon = Icons.Default.Favorite,
        content = {}
    ),
    History(
        title = "历史记录",
        icon = Icons.Default.MailOutline,
        content = {
            HistoryScreen().Content()
        }
    ),
    Download(
        title = "下载管理",
        icon = Icons.Default.Done,
        content = {
            DownloadScreen().Content()
        }
    ),
    Setting(
        title = "程序设置",
        icon = Icons.Default.Settings,
        content = {
            SettingScreen().Content()
        }
    ),
    Logout(
        title = "退出登录",
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        content = {
            val nav = LocalNavigator.currentOrThrow
            AlertDialog(
                onDismissRequest = {
                    nav.pop()
                },
                title = {
                    Text("确定退出登录？")
                },
                text = {
                    Text("这会清除您在本机上的登录状态，确定要这么做吗？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            nav.replaceAll(LoginScreen(exitLogin = true))
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            nav.replace(ProfileScreen(it))
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    )
}

@Composable
private fun SettingDrawerSheet(me: SimpleMeProfile, current: Int, onItemClick: (Int) -> Unit = {}) {
    ModalDrawerSheet {
        OutlinedCard(
            modifier = Modifier.padding(16.dp)
        ) {
            ListItem(headlineContent = {
                Text(me.name)
            }, leadingContent = {
                val nav = LocalNavigator.currentOrThrow
                IconButton(onClick = {
                    nav.pop()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "")
                }
            }, supportingContent = {
                Text("${me.pixivId}(${me.userId})")
            }, trailingContent = {
                ProgressedAsyncImage(
                    url = me.profileImageUrls.content, modifier = Modifier.size(45.dp)
                )

            })
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        LazyColumn(modifier = Modifier.padding(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(ProfileItem.entries.toTypedArray()) {
                val nav = LocalNavigator.currentOrThrow
                SettingItem(text = it.title, icon = it.icon, selected = it.ordinal == current) {
                    if (it.ordinal == ProfileItem.ExtendsFavorite.ordinal) {
                        nav.push(BookmarkScreen())
                        return@SettingItem
                    }
                    onItemClick(it.ordinal)
                }
            }
        }
    }
}

@Composable
private fun SettingItem(text: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit = {}) {
    NavigationDrawerItem(
        label = {
            Text(text)
        }, icon = {
            Icon(icon, "")
        }, selected = selected, onClick = onClick
    )

}