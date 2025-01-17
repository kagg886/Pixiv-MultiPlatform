package top.kagg886.pmf.ui.route.welcome

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.alorma.compose.settings.ui.SettingsSwitch
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import top.kagg886.pmf.*
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.component.SelectionCard
import top.kagg886.pmf.ui.component.guide.GuideScaffold
import top.kagg886.pmf.ui.component.icon.DarkMode
import top.kagg886.pmf.ui.component.icon.LightMode
import top.kagg886.pmf.ui.component.icon.SystemSuggest
import top.kagg886.pmf.ui.route.login.LoginScreen
import top.kagg886.pmf.ui.route.main.recommend.RecommendScreen
import top.kagg886.pmf.ui.route.welcome.WelcomeViewState.ConfigureSetting.*
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.ui.util.withLink
import top.kagg886.pmf.util.SerializedTheme

class WelcomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = navigator.koinNavigatorScreenModel<WelcomeModel>()
        val state by model.collectAsState()

        model.collectSideEffect {
            when (it) {
                WelcomeSideEffect.NavigateToMain -> {
                    val token = PixivConfig.pixiv_user
                    navigator.replace(
                        if (token == null) LoginScreen(true) else RecommendScreen()
                    )
                }
            }
        }
        WelcomeContent(model, state)
    }

    @Composable
    fun WelcomeContent(model: WelcomeModel, state0: WelcomeViewState) {
        when (state0) {
            WelcomeViewState.Loading -> {
                //安卓端无法进入时需要在这里提交副作用
                LaunchedEffect(Unit) {
                    model.initInCompose()
                }
            }

            is WelcomeViewState.ConfigureSetting -> {
                AnimatedContent(
                    targetState = state0,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }
                ) { state ->
                    GuideScaffold(
                        modifier = Modifier.fillMaxSize(),
                        title = {
                            Text(
                                when (state) {
                                    WELCOME -> "欢迎！"
                                    THEME -> "主题设置"
                                    BYPASS -> "SNI绕过"
                                    SHIELD -> "屏蔽配置"
                                    FINISH -> "大功告成！"
                                }
                            )
                        },
                        subTitle = {
                            Text("第${state.ordinal + 1}/${WelcomeViewState.ConfigureSetting.entries.size}步")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    model.nextStep()
                                }
                            ) {
                                Text("下一步")
                            }
                        },
                        skipButton = {
                            TextButton(
                                onClick = {
                                    model.skipAll()
                                }
                            ) {
                                Text("跳过设置")
                            }
                        },
                        backButton = {
                            OutlinedButton(
                                onClick = {
                                    model.goback()
                                },
                                enabled = state != WELCOME
                            ) {
                                Text("上一步")
                            }
                        }
                    ) {
                        WelcomeElementContent(model, state)
                    }

                }
            }
        }
    }

    @Composable
    private fun WelcomeElementContent(model: WelcomeModel, state: WelcomeViewState.ConfigureSetting) {
        val colors = MaterialTheme.colorScheme
        when (state) {
            WELCOME -> {
                Text(
                    buildAnnotatedString {
                        append("        ${BuildConfig.APP_NAME} 是一个基于 Compose Multiplatform 的跨平台第三方Pixiv客户端，由")
                        withLink(
                            colors = colors,
                            link = "https://github.com/kagg886/pixko",
                            display = " Pixko "
                        )
                        append("强力驱动。")

                        appendLine()

                        append("        在进行登录之前，让我们完成一些设置。这些设置于可能会随你所在的地区，网络环境而变动。")
                        append("如果您决定结束设置。请点击下方的 '跳过设置' 按钮，否则点按下一步以前往下一步设置。")

                        appendLine()

                        append("        准备好了吗？点击下一步以开始设置。")
                    }
                )
            }

            THEME -> {
                Text(
                    buildAnnotatedString {
                        append("        选择一份APP的主题吧！")

                    }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    var darkMode by LocalDarkSettings.current

                    LaunchedEffect(darkMode) {
                        AppConfig.darkMode = darkMode
                    }

                    SelectionCard(
                        select = darkMode == AppConfig.DarkMode.Light,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            darkMode = AppConfig.DarkMode.Light
                        }
                    ) {

                        var colorScheme by LocalColorScheme.current
                        ListItem(
                            leadingContent = {
                                Icon(LightMode, "")
                            },
                            headlineContent = {
                                Text("日间模式")
                            },
                            trailingContent = {
                                val scope = rememberCoroutineScope()
                                val snack = LocalSnackBarHost.current
                                LaunchedEffect(colorScheme) {
                                    AppConfig.colorScheme = colorScheme
                                }
                                val launcher = rememberFilePickerLauncher {
                                    if (it == null) {
                                        return@rememberFilePickerLauncher
                                    }

                                    scope.launch {
                                        val theme = kotlin.runCatching {
                                            val j = Json {
                                                ignoreUnknownKeys = true
                                            }
                                            Json.decodeFromString<JsonObject>(
                                                it.readBytes().decodeToString()
                                            )["schemes"]!!.jsonObject["light"]!!.jsonObject.let {
                                                j.decodeFromJsonElement<SerializedTheme>(it)
                                            }
                                        }
                                        if (theme.isFailure) {
                                            scope.launch {
                                                snack.showSnackbar("无法导入主题，原因：${theme.exceptionOrNull()!!.message}")
                                            }
                                            return@launch
                                        }
                                        colorScheme = theme.getOrThrow()
                                    }
                                }

                                AnimatedContent(colorScheme != null) {
                                    if (it) {
                                        IconButton(
                                            onClick = {
                                                colorScheme = null
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, null)
                                        }
                                        return@AnimatedContent
                                    }
                                    IconButton(
                                        onClick = {
                                            launcher.launch()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, null)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically)
                        )
                    }

                    SelectionCard(
                        select = darkMode == AppConfig.DarkMode.Dark,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            darkMode = AppConfig.DarkMode.Dark
                        }
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(DarkMode, "")
                            },
                            headlineContent = {
                                Text("夜间模式")
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically)
                        )
                    }

                    SelectionCard(
                        select = darkMode == AppConfig.DarkMode.System,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            darkMode = AppConfig.DarkMode.System
                        }
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(SystemSuggest, "")
                            },
                            headlineContent = {
                                Text("跟随系统")
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        buildAnnotatedString {
                            append("日间模式可以自定义主题色，主题文件可以在 ")
                            withLink(
                                colors = colors,
                                link = "https://material-foundation.github.io/material-theme-builder/",
                            )
                            append(" 处生成。(仅支持json格式的主题)")
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            BYPASS -> {
                Text(
                    buildAnnotatedString {
                        append("        在某些地区，由于政策限制，可能无法访问Pixiv。")
                        appendLine()
                        append("        该功能通过DoH返回任意Pixiv子域名的服务器，并直连其ip的方式，从而获得绕过封锁，直连Pixiv的能力。")
                        appendLine()
                        append("        要想启用该功能，请确保CloudFlare DoH服务在您的地区可用，并且Pixiv的ip地址未被封锁")
                    }
                )

                var byPassSni by remember {
                    mutableStateOf(AppConfig.byPassSNI)
                }
                LaunchedEffect(byPassSni) {
                    AppConfig.byPassSNI = byPassSni
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                ) {
                    SelectionCard(
                        select = byPassSni == true,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            byPassSni = true
                        }
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Default.Check, "")
                            },
                            headlineContent = {
                                Text("开启")
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically)
                        )
                    }

                    SelectionCard(
                        select = byPassSni == false,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            byPassSni = false
                        }
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Default.Delete, "")
                            },
                            headlineContent = {
                                Text("关闭")
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Card(Modifier.fillMaxWidth()) {
                    Text(
                        buildAnnotatedString {
                            appendLine("该功能不会影响内置浏览器的Pixiv登录功能。因此在登录Pixiv时，你仍然需要使用VPN等工具。")
                            appendLine("启用/禁用功能后，你需要重启客户端以生效")
                        }, modifier = Modifier.padding(8.dp)
                    )
                }
            }

            SHIELD -> {
                Text(
                    buildAnnotatedString {
                        append("        通过这三个问题，让我们帮您选择最适合您的屏蔽设置。更高级的小说屏蔽功能，请在登录后前往设置页面。")
                    }
                )
                Spacer(Modifier.height(16.dp))

                var likeR18 by remember {
                    mutableStateOf(true)
                }

                var likeR18G by remember {
                    mutableStateOf(true)
                }

                var likeAI by remember {
                    mutableStateOf(true)
                }

                LaunchedEffect(likeR18, likeR18G, likeAI) {
                    AppConfig.filterR18 = !likeR18
                    AppConfig.filterR18Novel = !likeR18

                    AppConfig.filterR18G = !likeR18G
                    AppConfig.filterR18GNovel = !likeR18G

                    AppConfig.filterAi = !likeAI
                    AppConfig.filterAiNovel = !likeAI
                }

                SettingsSwitch(
                    state = likeR18,
                    title = {
                        Text("您对 R18 作品的接受程度如何呢？")
                    },
                    subtitle = {
                        if (likeR18) {
                            Text("涩涩，好耶！")
                        } else {
                            Text("涩涩，达咩！")
                        }
                    },
                    modifier = Modifier.zIndex(0f)
                ) {
                    likeR18 = it
                    if (likeR18.not()) {
                        likeR18G = false
                    }
                }

                AnimatedVisibility(
                    visible = likeR18,
                    enter = expandVertically(),
                    exit =  shrinkVertically(),
                ) {
                    SettingsSwitch(
                        state = likeR18G,
                        title = {
                            Text("您对 R18G 作品的接受程度如何呢？")
                        },
                        modifier = Modifier.zIndex(-1f)
                    ) {
                        likeR18G = it
                    }
                }

                SettingsSwitch(
                    state = likeAI,
                    title = {
                        Text("您对 AI 作品的接受程度如何呢？")
                    },
                    subtitle = {
                        if (likeAI) {
                            Text("AI生成，好耶！")
                        } else {
                            Text("AI，达咩！")
                        }
                    }
                ) {
                    likeAI = it
                }
            }

            FINISH -> {
                Text(
                    buildAnnotatedString {
                        appendLine("        您已经完成了所有的设置。现在，我们将跳转到登录页面。这些设置可以在登录后前往设置页面进行修改。")
                        if (currentPlatform is Platform.Desktop) {
                            appendLine("        在初次登录前，我们会下载嵌入式浏览器以提供完整的登录体验，这会占用您大约300M的磁盘空间。")
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun ListItem(
        leadingContent: @Composable (() -> Unit)? = null,
        headlineContent: @Composable () -> Unit,
        trailingContent: @Composable (() -> Unit)? = null,
        modifier: Modifier = Modifier,
    ) {
        if (useWideScreenMode) {
            androidx.compose.material3.ListItem(
                leadingContent = leadingContent,
                headlineContent = headlineContent,
                trailingContent = trailingContent,
                modifier = modifier,
            )
            return
        }
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (leadingContent != null) {
                Spacer(Modifier.height(8.dp))
                leadingContent()
            }

            Spacer(Modifier.height(8.dp))
            headlineContent()

            if (trailingContent != null) {
                Spacer(Modifier.height(8.dp))
                trailingContent()
            }
        }
    }
}