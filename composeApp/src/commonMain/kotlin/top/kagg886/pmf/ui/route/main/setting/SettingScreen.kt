package top.kagg886.pmf.ui.route.main.setting

import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
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
import korlibs.io.net.MimeType
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.koin.ext.getFullName
import org.koin.mp.KoinPlatform.getKoin
import top.kagg886.pmf.LocalColorScheme
import top.kagg886.pmf.LocalDarkSettings
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.ui.component.settings.SettingsDropdownMenu
import top.kagg886.pmf.ui.component.settings.SettingsFileUpload
import top.kagg886.pmf.ui.component.settings.SettingsTextField
import top.kagg886.pmf.ui.route.login.v2.LoginScreen
import top.kagg886.pmf.ui.route.main.about.AboutScreen
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.util.*

class SettingScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (useWideScreenMode) {
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
                            }",
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

                val inNight =
                    darkMode == AppConfig.DarkMode.Dark || (darkMode == AppConfig.DarkMode.System && isSystemInDarkTheme())
                SettingsFileUpload(
                    title = { Text("设置主题") },
                    enabled = !inNight,
                    extensions = listOf("zip", "json"),
                    subTitle = {
                        AnimatedContent(
                            inNight,
                        ) {
                            if (it) {
                                Text("夜间模式的主题暂不支持设置")
                                return@AnimatedContent
                            }
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
                                                    textDecoration = TextDecoration.Underline,
                                                ),
                                            ),
                                        ),
                                    ) {
                                        append("https://material-foundation.github.io/material-theme-builder/")
                                    }
                                    appendLine("下载主题")
                                    append("导出的格式只有为json时才能正确解析！")
                                },
                            )
                        }
                    },
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
                    enabled = colorScheme != null,
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
                    },
                )
                AnimatedContent(
                    targetState = data,
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
                                },
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
                                },
                            )
                        }
                    }
                }

                var cacheSize by remember {
                    mutableStateOf(AppConfig.cacheSize)
                }
                LaunchedEffect(cacheSize) {
                    AppConfig.cacheSize = cacheSize
                }
                SettingsTextField(
                    title = {
                        Text("画廊本地缓存大小(重启生效)")
                    },
                    subTitle = {
                        Column {
                            Text("控制画廊页面的本地缓存大小,单位为MB。")
                            Text("当前值:${cacheSize.b}")
                            Text("点击此条目进行修改。")
                        }
                    },
                    dialogLabel = {
                        Text("最小值:64MB,最大值:2048MB")
                    },
                    value = cacheSize.b.mb.roundToLong().toString(),
                    onValueChange = {
                        var size = it.toLongOrNull() ?: 1024L
                        if (size !in 64.mb..2048.mb) {
                            size = 1024.mb.bytes
                        }
                        cacheSize = size
                    },
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
                    },
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
                    },
                )
                LaunchedEffect(filterR18G) {
                    AppConfig.filterR18G = filterR18G
                }
                SettingsSwitch(
                    state = filterR18G,
                    enabled = !filterR18, // 不过滤r18时启用
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
                    },
                )

                var gifSupport by remember {
                    mutableStateOf(AppConfig.gifSupport)
                }
                LaunchedEffect(gifSupport) {
                    AppConfig.gifSupport = gifSupport
                }
                SettingsSwitch(
                    state = gifSupport,
                    title = {
                        Text("动态图片支持(实验性)")
                    },
                    subtitle = {
                        Text("开启此选项后，若识别到这是一张动图，则会自动转为gif并展示")
                    },
                    onCheckedChange = {
                        gifSupport = it
                    },
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
                    },
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
                    },
                )
                LaunchedEffect(filterR18GNovel) {
                    AppConfig.filterR18GNovel = filterR18GNovel
                }
                SettingsSwitch(
                    state = filterR18GNovel,
                    enabled = !filterR18Novel, // 不过滤r18时启用
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
                    },
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
                        Text("去除多余的空行，并首行缩进${textSize * 2}sp")
                    },
                    onCheckedChange = {
                        autoTypo = it
                    },
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
                    },
                )
                var filterLongTag by remember {
                    mutableStateOf(AppConfig.filterLongTag)
                }
                var filterLongTagLength by remember {
                    mutableStateOf(AppConfig.filterLongTagMinLength)
                }
                LaunchedEffect(filterLongTag) {
                    AppConfig.filterLongTag = filterLongTag
                    if (!filterLongTag) {
                        filterLongTagLength = 15
                    }
                }
                SettingsSwitch(
                    state = filterLongTag,
                    title = {
                        Text("过滤长TAG小说")
                    },
                    subtitle = {
                        Text("这类小说内部的主题很可能与TAG不相关")
                    },
                    onCheckedChange = {
                        filterLongTag = it
                    },
                )
                SettingsSlider(
                    enabled = filterLongTag,
                    title = {
                        Text("TAG最大长度")
                    },
                    subtitle = {
                        Column {
                            Text("当某一TAG长度超出这个值时，将不会显示")
                            Text("当前值:$filterLongTagLength")
                        }
                    },
                    value = filterLongTagLength.toFloat(),
                    valueRange = 5f..25f,
                    onValueChange = {
                        filterLongTagLength = it.roundToInt()
                    },
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
                    },
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
                    },
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
                    },
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
                    },
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
                    },
                )
            }
            SettingsGroup(title = { Text(text = "网络设置") }) {
                var bypassSetting by remember {
                    mutableStateOf(AppConfig.bypassSettings)
                }
                val bypassSettingsKey by remember(bypassSetting) {
                    derivedStateOf { bypassSetting::class.getFullName() }
                }
                LaunchedEffect(bypassSetting) {
                    AppConfig.bypassSettings = bypassSetting
                }

                SettingsDropdownMenu(
                    title = { Text("直连方案") },
                    subTitle = { Text("在部分地区无法访问Pixiv API，可以更改此设置以解除限制。") },
                    data = listOf(
                        AppConfig.BypassSetting.None,
                        AppConfig.BypassSetting.Proxy(),
                        AppConfig.BypassSetting.SNIReplace(),
                    ),
                    current = bypassSetting,
                    onSelected = {
                        bypassSetting = it
                    },
                    optionsFormat = {
                        when (it) {
                            AppConfig.BypassSetting.None -> "无"
                            is AppConfig.BypassSetting.Proxy -> "代理"
                            is AppConfig.BypassSetting.SNIReplace -> "SNI替换"
                        }
                    },
                )

                AnimatedContent(
                    targetState = bypassSettingsKey,
                    transitionSpec = { expandVertically() togetherWith shrinkVertically() },
                ) {
                    Column {
                        when (val readOnlySettings = bypassSetting) {
                            AppConfig.BypassSetting.None -> {}
                            is AppConfig.BypassSetting.Proxy -> {
                                SettingsDropdownMenu(
                                    title = { Text("代理类型") },
                                    current = readOnlySettings.type,
                                    data = AppConfig.BypassSetting.Proxy.ProxyType.entries,
                                    onSelected = {
                                        bypassSetting = readOnlySettings.copy(type = it)
                                    },
                                )

                                SettingsTextField(
                                    title = { Text("代理地址") },
                                    value = readOnlySettings.host,
                                    onValueChange = {
                                        bypassSetting = readOnlySettings.copy(host = it)
                                    },
                                )
                                SettingsTextField(
                                    title = { Text("代理端口") },
                                    value = readOnlySettings.port.toString(),
                                    onValueChange = {
                                        val port = it.toIntOrNull() ?: return@SettingsTextField
                                        bypassSetting = readOnlySettings.copy(port = port)
                                    },
                                )
                            }

                            is AppConfig.BypassSetting.SNIReplace -> {
                                val snack = LocalSnackBarHost.current
                                val scope = rememberCoroutineScope()
                                SettingsTextField(
                                    title = { Text("DoH地址") },
                                    subTitle = {
                                        Text(
                                            buildAnnotatedString {
                                                appendLine("通过DoH来解析Pixiv真实ip。")
                                                appendLine("若默认DoH不可用，请尝试更换此值为合法且能解析pixivvision.net的DoH服务器。")
                                            },
                                        )
                                    },
                                    value = readOnlySettings.url,
                                    onValueChange = {
                                        bypassSetting = readOnlySettings.copy(url = it)
                                    },
                                )

                                SettingsTextField(
                                    title = { Text("DoH超时时间") },
                                    subTitle = {
                                        Text(
                                            buildAnnotatedString {
                                                appendLine("设置DoH的超时时间，单位为秒")
                                                appendLine("若DoH解析失败，请尝试调高DoH解析时间以给予更多超时时间以解析DoH")
                                            },
                                        )
                                    },
                                    value = readOnlySettings.dohTimeout.toString(),
                                    onValueChange = {
                                        val data = it.toIntOrNull() ?: run {
                                            scope.launch {
                                                snack.showSnackbar("格式错误")
                                            }
                                            return@run 5
                                        }
                                        bypassSetting = readOnlySettings.copy(dohTimeout = data)
                                    },
                                )
                                SettingsSwitch(
                                    state = readOnlySettings.nonStrictSSL,
                                    title = {
                                        Text("忽略SSL错误")
                                    },
                                    subtitle = {
                                        Text("关闭后将不会忽略非严格SSL错误(可能造成DoH解析失败)，一般不会调整此设置。")
                                    },
                                    onCheckedChange = {
                                        bypassSetting = readOnlySettings.copy(nonStrictSSL = it)
                                    },
                                )
                                SettingsTextField(
                                    title = { Text("内置IP池列表") },
                                    subTitle = {
                                        Text(
                                            buildAnnotatedString {
                                                appendLine("DoH不可用时直接提供此表中的IP")
                                                appendLine("作为最后的防御手段，该ip一定要可以直连")
                                            },
                                        )
                                    },
                                    value = Json.encodeToString(readOnlySettings.fallback),
                                    onValueChange = {
                                        val fallback = try {
                                            Json.decodeFromString<Map<String, List<String>>>(it)
                                        } catch (e: Exception) {
                                            scope.launch {
                                                snack.showSnackbar("格式错误")
                                            }
                                            return@SettingsTextField
                                        }
                                        bypassSetting = readOnlySettings.copy(fallback = fallback)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            SettingsGroup(title = { Text(text = "登录会话") }) {
                val clip = LocalClipboardManager.current
                val scope = rememberCoroutineScope()
                val snack = LocalSnackBarHost.current
                SettingsMenuLink(
                    title = {
                        Text("导出登录会话")
                    },
                    subtitle = {
                        Text("可以使用此会话登录多个Pixiv第三方客户端。")
                    },
                    onClick = {
                        scope.launch {
                            clip.setText(
                                buildAnnotatedString {
                                    append(PixivConfig.refreshToken)
                                },
                            )
                            snack.showSnackbar("已将会话信息已复制到剪切板。注意：请勿将此会话发送到公开场合，这可能会导致您的账户被恶意修改。")
                        }
                    },
                )

                var show by remember { mutableStateOf(false) }
                if (show) {
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
                                    nav.replaceAll(LoginScreen(true))
                                },
                            ) {
                                Text("确定")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    show = false
                                },
                            ) {
                                Text("取消")
                            }
                        },
                    )
                }
                SettingsMenuLink(
                    title = {
                        Text("退出登录")
                    },
                    subtitle = {
                        Text("清空会话并进入登录页面")
                    },
                    onClick = {
                        show = true
                    },
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
                        Text("导出单次日志")
                    },
                    subtitle = {
                        Text("可能会很卡")
                    },
                    onClick = {
                        scope.launch {
                            shareFile(cachePath.resolve("log").resolve("latest.log"), mime = MimeType.TEXT_PLAIN.mime)
                        }
                    },
                )
                SettingsMenuLink(
                    title = {
                        Text("清空日志归档")
                    },
                    subtitle = {
                        Text("日志可能会占用存储空间")
                    },
                    onClick = {
                        scope.launch {
                            runCatching {
                                // windows平台无法删除正在hold的文件
                                cachePath.resolve("log").deleteRecursively()
                            }
                        }
                    },
                )
                SettingsMenuLink(
                    title = {
                        Text("导出全部日志")
                    },
                    subtitle = {
                        Text("一定会很卡")
                    },
                    onClick = {
                        scope.launch {
                            shareFile(cachePath.resolve("log").zip())
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
                    },
                )

                AnimatedVisibility(
                    checkUpdateOnStart,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    SettingsSwitch(
                        enabled = checkUpdateOnStart,
                        state = showCheckSuccessToast,
                        title = {
                            Text("检查为最新版本时显示Toast")
                        },
                        onCheckedChange = {
                            showCheckSuccessToast = it
                        },
                    )

                    SettingsSwitch(
                        enabled = checkUpdateOnStart,
                        state = showCheckFailedToast,
                        title = {
                            Text("检查失败时显示Toast")
                        },
                        onCheckedChange = {
                            showCheckFailedToast = it
                        },
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
                    },
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
                    },
                )
            }
        }
    }
}
