package top.kagg886.pmf.ui.route.main.setting

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.SettingsSwitch
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin
import top.kagg886.pmf.LocalThemeSaver
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.ui.route.main.about.AboutScreen
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.b
import top.kagg886.pmf.ui.util.mb
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds


class SettingScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
    @Composable
    override fun Content() {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (currentPlatform.useWideScreenMode) {
                TopAppBar(
                    title = {
                        Text("设置")
                    },
                )
            }
            SettingsGroup(title = { Text("外观") }) {
                var theme by LocalThemeSaver.current
                var expand by remember { mutableStateOf(false) }
                LaunchedEffect(theme) {
                    AppConfig.darkMode = theme
                    expand = false
                }
                SettingsTileScaffold(
                    title = {
                        Text("显示模式")
                    },
                    subtitle = {
                        Text(
                            "当前为:${
                                when (theme) {
                                    AppConfig.DarkMode.System -> "跟随系统"
                                    AppConfig.DarkMode.Light -> "日间模式"
                                    AppConfig.DarkMode.Dark -> "夜间模式"
                                }
                            }"
                        )
                    },
                    modifier = Modifier.clickable {
                        expand = true
                    },
                ) {
                    DropdownMenu(
                        expanded = expand,
                        onDismissRequest = {
                            expand = false
                        },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("跟随系统")
                            },
                            onClick = {
                                theme = AppConfig.DarkMode.System
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text("始终日间")
                            },
                            onClick = {
                                theme = AppConfig.DarkMode.Light
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text("始终夜间")
                            },
                            onClick = {
                                theme = AppConfig.DarkMode.Dark
                            }
                        )
                    }
                }
            }
            SettingsGroup(
                title = { Text(text = "画廊设置") },
            ) {
                var data by remember { mutableStateOf(AppConfig.galleryOptions) }
                var expand by remember { mutableStateOf(false) }
                LaunchedEffect(data) {
                    AppConfig.galleryOptions = data
                    expand = false
                }
                SettingsTileScaffold(
                    title = {
                        Text("列数计算方式")
                    },
                    subtitle = {
                        Text("指定程序以什么样的方式计算画廊的列数")
                    },
                    modifier = Modifier.clickable {
                        expand = true
                    },
                ) {
                    Text(
                        when (data) {
                            is AppConfig.Gallery.FixColumnCount -> "固定列数"
                            is AppConfig.Gallery.FixWidth -> "根据列宽计算"
                        }
                    )
                    DropdownMenu(
                        expanded = expand,
                        onDismissRequest = {
                            expand = false
                        },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("固定列数")
                            },
                            onClick = {
                                data = AppConfig.Gallery.FixColumnCount(3)
                                expand = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text("根据列宽计算")
                            },
                            onClick = {
                                data = AppConfig.Gallery.FixWidth(100)
                                expand = false
                            }
                        )
                    }
                }

                AnimatedContent(
                    targetState = data
                ) {
                    when (it) {
                        is AppConfig.Gallery.FixColumnCount -> {
                            var defaultGalleryWidth by remember { mutableStateOf(it.size.toFloat()) }
                            LaunchedEffect(defaultGalleryWidth) {
                                AppConfig.galleryOptions = it.copy(size = defaultGalleryWidth.toInt())
                            }
                            SettingsSlider(
                                title = {
                                    Text("画廊列数")
                                },
                                subtitle = {
                                    Text("控制画廊页面的列数,当前值:$defaultGalleryWidth")
                                },
                                value = defaultGalleryWidth,
                                valueRange = 2f..5f,
                                steps = 2,
                                onValueChange = {
                                    defaultGalleryWidth = it
                                }
                            )
                        }
                        is AppConfig.Gallery.FixWidth -> {
                            var defaultGalleryWidth by remember { mutableStateOf(it.size) }
                            LaunchedEffect(defaultGalleryWidth) {
                                AppConfig.galleryOptions = it.copy(size = defaultGalleryWidth)
                            }
                            SettingsSlider(
                                title = {
                                    Text("单列宽度")
                                },
                                subtitle = {
                                    Text("控制画廊页面单列的大小,当前值:$defaultGalleryWidth")
                                },
                                value = defaultGalleryWidth.toFloat(),
                                valueRange = 50f..1000f,
                                steps = 949,
                                onValueChange = {
                                    defaultGalleryWidth = it.roundToInt()
                                }
                            )
                        }
                    }

                }


                var cacheSize by remember {
                    mutableStateOf(AppConfig.cacheSize.toFloat())
                }
                LaunchedEffect(cacheSize) {
                    snapshotFlow { cacheSize }.debounce(1.seconds).collectLatest {
                        AppConfig.cacheSize = cacheSize.toLong()
                    }
                }
                SettingsSlider(
                    title = {
                        Text("画廊本地缓存大小(重启生效)")
                    },
                    subtitle = {
                        Column {
                            Text("控制画廊页面的本地缓存大小,单位为字节。")
                            Text("当前值:${cacheSize.b}")
                        }
                    },
                    value = cacheSize,
                    valueRange = 10.mb.bytes.toFloat()..2048.mb.bytes.toFloat(),
                    onValueChange = {
                        cacheSize = it
                    }
                )

                var filterAi by remember {
                    mutableStateOf(AppConfig.filterAi)
                }
                LaunchedEffect(filterAi) {
                    AppConfig.filterAi = filterAi
                }
                SettingsSwitch(
                    state = filterAi,
                    title = {
                        Text("AI插画过滤")
                    },
                    subtitle = {
                        Text("若在服务端设置了过滤，则可以不用开启")
                    },
                    onCheckedChange = {
                        filterAi = it
                    }
                )

                var filterR18 by remember {
                    mutableStateOf(AppConfig.filterR18)
                }
                var filterR18G by remember {
                    mutableStateOf(AppConfig.filterR18G)
                }
                LaunchedEffect(filterR18) {
                    AppConfig.filterR18 = filterR18
                }
                SettingsSwitch(
                    state = filterR18,
                    title = {
                        Text("R18插画过滤")
                    },
                    subtitle = {
                        Column {
                            Text("建议关闭官方的R18过滤功能并开启这个")
                            Text("这样可以防止R15-R18之间的插画被误杀(暴露图片的商单仍会被过滤)")
                        }
                    },
                    onCheckedChange = {
                        filterR18 = it
                    }
                )
                LaunchedEffect(filterR18G) {
                    AppConfig.filterR18G = filterR18G
                }
                SettingsSwitch(
                    state = filterR18G,
                    enabled = !filterR18, //不过滤r18时启用
                    title = {
                        Text("R18G插画过滤")
                    },
                    subtitle = {
                        Column {
                            Text("会过滤关于R18G的内容。")
                            Text("仅在关闭R18过滤后可选择")
                        }
                    },
                    onCheckedChange = {
                        filterR18G = it
                    }
                )
            }
            SettingsGroup(
                title = { Text("小说设置") }
            ) {
                var filterAiNovel by remember {
                    mutableStateOf(AppConfig.filterAiNovel)
                }
                LaunchedEffect(filterAiNovel) {
                    AppConfig.filterAiNovel = filterAiNovel
                }
                SettingsSwitch(
                    state = filterAiNovel,
                    title = {
                        Text("AI小说过滤")
                    },
                    subtitle = {
                        Text("若在服务端设置了过滤，则可以不用开启")
                    },
                    onCheckedChange = {
                        filterAiNovel = it
                    }
                )

                var filterR18Novel by remember {
                    mutableStateOf(AppConfig.filterR18Novel)
                }
                var filterR18GNovel by remember {
                    mutableStateOf(AppConfig.filterR18GNovel)
                }
                LaunchedEffect(filterR18Novel) {
                    AppConfig.filterR18Novel = filterR18Novel
                }
                SettingsSwitch(
                    state = filterR18Novel,
                    title = {
                        Text("R18小说过滤")
                    },
                    subtitle = {
                        Column {
                            Text("建议关闭官方的R18过滤功能并开启这个")
                            Text("这样可以防止R15-R18之间的插画被误杀(暴露图片的商单仍会被过滤)")
                        }
                    },
                    onCheckedChange = {
                        filterR18Novel = it
                    }
                )
                LaunchedEffect(filterR18GNovel) {
                    AppConfig.filterR18GNovel = filterR18GNovel
                }
                SettingsSwitch(
                    state = filterR18GNovel,
                    enabled = !filterR18Novel, //不过滤r18时启用
                    title = {
                        Text("R18G小说过滤")
                    },
                    subtitle = {
                        Column {
                            Text("会过滤关于R18G的内容。")
                            Text("仅在关闭R18过滤后可选择")
                        }
                    },
                    onCheckedChange = {
                        filterR18GNovel = it
                    }
                )

                var autoTypo by remember {
                    mutableStateOf(AppConfig.autoTypo)
                }
                LaunchedEffect(autoTypo) {
                    AppConfig.autoTypo = autoTypo
                }
                SettingsSwitch(
                    state = autoTypo,
                    title = {
                        Text("自动排版")
                    },
                    subtitle = {
                        Text("首行缩进24sp，行高24sp")
                    },
                    onCheckedChange = {
                        autoTypo = it
                    }
                )
                var filterShortNovel by remember {
                    mutableStateOf(AppConfig.filterShortNovel)
                }
                var filterShortNovelLength by remember {
                    mutableStateOf(AppConfig.filterShortNovelMaxLength)
                }
                LaunchedEffect(filterShortNovel) {
                    AppConfig.filterShortNovel = filterShortNovel
                    if (!filterShortNovel) {
                        filterShortNovelLength = 100
                    }
                }
                SettingsSwitch(
                    state = filterShortNovel,
                    title = {
                        Text("过滤极短小说")
                    },
                    subtitle = {
                        Text("这类小说内部很有可能是引流广告")
                    },
                    onCheckedChange = {
                        filterShortNovel = it
                    }
                )
                SettingsSlider(
                    enabled = filterShortNovel,
                    title = {
                        Text("小说过滤长度")
                    },
                    subtitle = {
                        Column {
                            Text("当小说长度小于这个值时，将不会显示")
                            Text("当前值:$filterShortNovelLength")
                        }
                    },
                    value = filterShortNovelLength.toFloat(),
                    valueRange = 30f..1000f,
                    steps = 968,
                    onValueChange = {
                        filterShortNovelLength = it.roundToInt()
                    }
                )
            }
            SettingsGroup(
                title = { Text(text = "历史记录") }
            ) {
                var recordIllustHistory by remember {
                    mutableStateOf(AppConfig.recordIllustHistory)
                }
                LaunchedEffect(recordIllustHistory) {
                    AppConfig.recordIllustHistory = recordIllustHistory
                }
                SettingsSwitch(
                    state = recordIllustHistory,
                    title = {
                        Text("记录插画浏览记录")
                    },
                    subtitle = {
                        Text("关闭后将不会记录插画历史")
                    },
                    onCheckedChange = {
                        recordIllustHistory = it
                    }
                )

                var recordNovelHistory by remember {
                    mutableStateOf(AppConfig.recordNovelHistory)
                }
                LaunchedEffect(recordNovelHistory) {
                    AppConfig.recordNovelHistory = recordNovelHistory
                }
                SettingsSwitch(
                    state = recordNovelHistory,
                    title = {
                        Text("记录小说浏览记录")
                    },
                    subtitle = {
                        Text("关闭后将不会记录小说历史")
                    },
                    onCheckedChange = {
                        recordNovelHistory = it
                    }
                )

                var recordSearchHistory by remember {
                    mutableStateOf(AppConfig.recordSearchHistory)
                }
                LaunchedEffect(recordSearchHistory) {
                    AppConfig.recordSearchHistory = recordSearchHistory
                }
                SettingsSwitch(
                    state = recordSearchHistory,
                    title = {
                        Text("记录搜索记录")
                    },
                    subtitle = {
                        Text("关闭后将不会记录搜索记录")
                    },
                    onCheckedChange = {
                        recordSearchHistory = it
                    }
                )
            }
            SettingsGroup(
                title = { Text(text = "网络设置") }
            ) {
                var byPassSni by remember {
                    mutableStateOf(AppConfig.byPassSNI)
                }
                LaunchedEffect(byPassSni) {
                    AppConfig.byPassSNI = byPassSni
                }
                SettingsSwitch(
                    state = byPassSni,
                    title = {
                        Text("SNI Bypass(重启生效)")
                    },
                    subtitle = {
                        Text("绕过Pixiv SNI Checker，可实现免VPN直连。若拥有梯子环境请关闭此功能以提高加载速度")
                    },
                    onCheckedChange = {
                        byPassSni = it
                    }
                )
            }
            SettingsGroup(title = { Text("高级") }) {
                SettingsMenuLink(
                    title = {
                        Text("主线程抛出异常")
                    },
                    subtitle = {
                        Text("调试用，没事别点")
                    },
                    onClick = {
                        throw RuntimeException("测试异常，请不要反馈。")
                    },
                )
                val scope = rememberCoroutineScope()
                SettingsMenuLink(
                    title = {
                        Text("协程内抛出异常")
                    },
                    subtitle = {
                        Text("调试用，没事别点")
                    },
                    onClick = {
                        scope.launch {
                            throw RuntimeException("测试异常，请不要反馈。")
                        }
                    },
                )
                SettingsMenuLink(
                    title = {
                        Text("子线程抛出异常")
                    },
                    subtitle = {
                        Text("调试用，没事别点")
                    },
                    onClick = {
                        thread {
                            throw RuntimeException("测试异常，请不要反馈。")
                        }
                    },
                )
            }
            SettingsGroup(title = { Text("更新") }) {
                val model = remember {
                    getKoin().get<UpdateCheckViewModel>()
                }

                var showCheckSuccessToast by remember {
                    mutableStateOf(AppConfig.checkSuccessToast)
                }
                LaunchedEffect(showCheckSuccessToast) {
                    AppConfig.checkSuccessToast = showCheckSuccessToast
                }

                var showCheckFailedToast by remember {
                    mutableStateOf(AppConfig.checkFailedToast)
                }
                LaunchedEffect(showCheckFailedToast) {
                    AppConfig.checkFailedToast = showCheckFailedToast
                }

                var checkUpdateOnStart by remember {
                    mutableStateOf(AppConfig.checkUpdateOnStart)
                }
                LaunchedEffect(checkUpdateOnStart) {
                    AppConfig.checkUpdateOnStart = checkUpdateOnStart
                    if (!checkUpdateOnStart) {
                        showCheckSuccessToast = false
                        showCheckFailedToast = true
                    }
                }
                SettingsSwitch(
                    state = checkUpdateOnStart,
                    title = {
                        Text("启动时检查更新")
                    },
                    onCheckedChange = {
                        checkUpdateOnStart = it
                    }
                )

                if (checkUpdateOnStart) {
                    SettingsSwitch(
                        enabled = checkUpdateOnStart,
                        state = showCheckSuccessToast,
                        title = {
                            Text("检查为最新版本时显示Toast")
                        },
                        onCheckedChange = {
                            showCheckSuccessToast = it
                        }
                    )

                    SettingsSwitch(
                        enabled = checkUpdateOnStart,
                        state = showCheckFailedToast,
                        title = {
                            Text("检查失败时显示Toast")
                        },
                        onCheckedChange = {
                            showCheckFailedToast = it
                        }
                    )
                }


                SettingsMenuLink(
                    title = {
                        Text("检查更新")
                    },
                    onClick = {
                        model.checkUpdate(true)
                    },
                    icon = {
                        Icon(Icons.Default.Build, "")
                    }
                )
                val nav = LocalNavigator.currentOrThrow
                SettingsMenuLink(
                    title = {
                        Text("关于")
                    },
                    onClick = {
                        nav.push(AboutScreen())
                    },
                    icon = {
                        Icon(Icons.Default.Info, "")
                    }
                )
            }
        }
    }
}


