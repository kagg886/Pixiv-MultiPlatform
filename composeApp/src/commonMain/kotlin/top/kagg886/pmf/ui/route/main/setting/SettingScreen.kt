package top.kagg886.pmf.ui.route.main.setting

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.java.KoinJavaComponent.getKoin
import top.kagg886.pmf.LocalColorScheme
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.LocalDarkSettings
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.ui.component.settings.SettingsDropdownMenu
import top.kagg886.pmf.ui.component.settings.SettingsFileUpload
import top.kagg886.pmf.ui.component.settings.SettingsTextField
import top.kagg886.pmf.ui.route.main.about.AboutScreen
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.b
import top.kagg886.pmf.ui.util.mb
import top.kagg886.pmf.util.SerializedTheme
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
                var darkMode by LocalDarkSettings.current

                LaunchedEffect(darkMode) {
                    AppConfig.darkMode = darkMode
                }

                SettingsDropdownMenu<AppConfig.DarkMode>(
                    title = { Text("显示模式") },
                    subTitle = {
                        Text(
                            "当前为:${
                                when (darkMode) {
                                    AppConfig.DarkMode.System -> "跟随系统"
                                    AppConfig.DarkMode.Light -> "日间模式"
                                    AppConfig.DarkMode.Dark -> "夜间模式"
                                }
                            }"
                        )
                    },
                    optionsFormat = {
                        when (it) {
                            AppConfig.DarkMode.System -> "跟随系统"
                            AppConfig.DarkMode.Light -> "日间模式"
                            AppConfig.DarkMode.Dark -> "夜间模式"
                        }
                    },
                    current = darkMode,
                    data = AppConfig.DarkMode.entries,
                    onSelected = {
                        darkMode = it
                    },
                )

                val scope = rememberCoroutineScope()
                val snack = LocalSnackBarHost.current

                var colorScheme by LocalColorScheme.current
                LaunchedEffect(colorScheme) {
                    AppConfig.colorScheme = colorScheme
                }

                SettingsFileUpload(
                    title = { Text("设置主题") },
                    subTitle = {
                        val colors = MaterialTheme.colorScheme
                        Text(
                            buildAnnotatedString {
                                append("请前往")
                                withLink(
                                    LinkAnnotation.Url(
                                        url = "https://material-foundation.github.io/material-theme-builder/",
                                        styles = TextLinkStyles(
                                            style = SpanStyle(color = colors.primary),
                                            hoveredStyle = SpanStyle(
                                                color = colors.primaryContainer,
                                                textDecoration = TextDecoration.Underline
                                            ),
                                        )
                                    ),
                                ) {
                                    append("https://material-foundation.github.io/material-theme-builder/")
                                }
                                appendLine("下载主题")
                                append("导出的格式只有为json时才能正确解析！")
                            }
                        )
                    }
                ) {
                    val theme = kotlin.runCatching {
                        val j = Json {
                            ignoreUnknownKeys = true
                        }
                        Json.decodeFromString<JsonObject>(it.decodeToString())["schemes"]!!.jsonObject["light"]!!.jsonObject.let {
                            j.decodeFromJsonElement<SerializedTheme>(it)
                        }
                    }
                    if (theme.isFailure) {
                        scope.launch {
                            snack.showSnackbar("无法导入主题，原因：${theme.exceptionOrNull()!!.message}")
                        }
                        return@SettingsFileUpload
                    }
                    colorScheme = theme.getOrThrow()
                }

                SettingsMenuLink(
                    title = { Text("还原主题") },
                    subtitle = { Text("将主题重置为默认") },
                    onClick = { colorScheme = null },
                    enabled = colorScheme != null
                )
            }
            SettingsGroup(title = { Text(text = "画廊设置") }) {
                var data by remember { mutableStateOf(AppConfig.galleryOptions) }
                LaunchedEffect(data) {
                    AppConfig.galleryOptions = data
                }
                SettingsDropdownMenu(
                    title = { Text("列数计算方式") },
                    subTitle = { Text("指定程序以什么样的方式计算画廊的列数") },
                    optionsFormat = {
                        when (it) {
                            is AppConfig.Gallery.FixColumnCount -> "固定列数"
                            is AppConfig.Gallery.FixWidth -> "根据列宽计算"
                        }
                    },
                    current = data,
                    data = listOf(AppConfig.Gallery.FixColumnCount(3), AppConfig.Gallery.FixWidth(100)),
                    onSelected = {
                        data = it
                    }
                )
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
            SettingsGroup(title = { Text("小说设置") }) {
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
                var textSize by remember {
                    mutableStateOf(AppConfig.textSize)
                }
                LaunchedEffect(textSize) {
                    AppConfig.textSize = textSize
                }
                SettingsSwitch(
                    state = autoTypo,
                    title = {
                        Text("自动排版")
                    },
                    subtitle = {
                        Text("首行缩进${textSize * 2}sp")
                    },
                    onCheckedChange = {
                        autoTypo = it
                    }
                )


                SettingsSlider(
                    value = textSize.toFloat(),
                    valueRange = 8f..40f,
                    onValueChange = {
                        textSize = it.roundToInt()
                    },
                    title = {
                        Text("正文大小")
                    },
                    subtitle = {
                        Text("${textSize}sp", fontSize = textSize.sp)
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
            SettingsGroup(title = { Text(text = "历史记录") }) {
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
            SettingsGroup(title = { Text(text = "网络设置") }) {
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

                var customPixivImageHost by remember {
                    mutableStateOf(AppConfig.customPixivImageHost)
                }
                LaunchedEffect(customPixivImageHost) {
                    AppConfig.customPixivImageHost = customPixivImageHost
                }
                val snack = LocalSnackBarHost.current
                val scope = rememberCoroutineScope()
                SettingsTextField(
                    value = customPixivImageHost,
                    onValueChange = {
                        if (it.isEmpty()) {
                            customPixivImageHost = ""
                            scope.launch {
                                snack.showSnackbar("已关闭自定义Pixiv Image代理")
                            }
                            return@SettingsTextField
                        }
                        val url = it.toHttpUrlOrNull()
                        if (url == null) {
                            scope.launch {
                                snack.showSnackbar("请输入正确的URL")
                            }
                            return@SettingsTextField
                        }
                        customPixivImageHost = it
                    },
                    title = {
                        Text("自定义Pixiv Image代理")
                    },
                    subTitle = {
                        Text("替换pixiv图片直链的url-host以提高加载速度。\n留空则禁用此属性")
                    },
                    dialogPlaceHolder = {
                        Text("e.g. i.pximg.net")
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


