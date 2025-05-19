package top.kagg886.pmf.ui.route.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
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
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.LocalColorScheme
import top.kagg886.pmf.LocalDarkSettings
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.NavigationItem
import top.kagg886.pmf.Res
import top.kagg886.pmf.ai_acceptance
import top.kagg886.pmf.ai_allowed
import top.kagg886.pmf.ai_blocked
import top.kagg886.pmf.app_language
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.bypass_intro
import top.kagg886.pmf.bypass_note
import top.kagg886.pmf.day_mode
import top.kagg886.pmf.follow_system
import top.kagg886.pmf.import_theme_fail
import top.kagg886.pmf.next_step
import top.kagg886.pmf.night_mode
import top.kagg886.pmf.no_bypass
import top.kagg886.pmf.previous_step
import top.kagg886.pmf.r18_acceptance
import top.kagg886.pmf.r18_allowed
import top.kagg886.pmf.r18_blocked
import top.kagg886.pmf.r18g_acceptance
import top.kagg886.pmf.setup_complete
import top.kagg886.pmf.setup_finish_note
import top.kagg886.pmf.setup_finish_note_simple
import top.kagg886.pmf.shield_config
import top.kagg886.pmf.shield_intro
import top.kagg886.pmf.skip_setup
import top.kagg886.pmf.sni_bypass
import top.kagg886.pmf.step_number
import top.kagg886.pmf.theme_info
import top.kagg886.pmf.theme_info_after_url
import top.kagg886.pmf.theme_info_url
import top.kagg886.pmf.theme_intro
import top.kagg886.pmf.theme_setting
import top.kagg886.pmf.ui.component.SelectionCard
import top.kagg886.pmf.ui.component.guide.GuideScaffold
import top.kagg886.pmf.ui.component.icon.DarkMode
import top.kagg886.pmf.ui.component.icon.LightMode
import top.kagg886.pmf.ui.component.icon.SystemSuggest
import top.kagg886.pmf.ui.route.login.v2.LoginScreen
import top.kagg886.pmf.ui.route.welcome.WelcomeViewState.ConfigureSetting.BYPASS
import top.kagg886.pmf.ui.route.welcome.WelcomeViewState.ConfigureSetting.FINISH
import top.kagg886.pmf.ui.route.welcome.WelcomeViewState.ConfigureSetting.LANGUAGE
import top.kagg886.pmf.ui.route.welcome.WelcomeViewState.ConfigureSetting.SHIELD
import top.kagg886.pmf.ui.route.welcome.WelcomeViewState.ConfigureSetting.THEME
import top.kagg886.pmf.ui.route.welcome.WelcomeViewState.ConfigureSetting.WELCOME
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.ui.util.withLink
import top.kagg886.pmf.unknown_error
import top.kagg886.pmf.use_proxy
import top.kagg886.pmf.use_sni_bypass
import top.kagg886.pmf.util.ComposeI18N
import top.kagg886.pmf.util.SerializedTheme
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.welcome
import top.kagg886.pmf.welcome_text
import top.kagg886.pmf.welcome_text_after_pixko

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
                        if (token == null) LoginScreen(true) else NavigationItem.RECOMMEND(),
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
                // 安卓端无法进入时需要在这里提交副作用
                LaunchedEffect(Unit) {
                    model.initInCompose()
                }
            }

            is WelcomeViewState.ConfigureSetting -> {
                AnimatedContent(
                    targetState = state0,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                ) { state ->
                    GuideScaffold(
                        modifier = Modifier.fillMaxSize(),
                        title = {
                            Text(
                                when (state) {
                                    LANGUAGE  -> stringResource(Res.string.app_language)
                                    WELCOME -> stringResource(Res.string.welcome)
                                    THEME -> stringResource(Res.string.theme_setting)
                                    BYPASS -> stringResource(Res.string.sni_bypass)
                                    SHIELD -> stringResource(Res.string.shield_config)
                                    FINISH -> stringResource(Res.string.setup_complete)
                                },
                            )
                        },
                        subTitle = {
                            Text(
                                stringResource(
                                    Res.string.step_number,
                                    (state.ordinal + 1).toString(),
                                    WelcomeViewState.ConfigureSetting.entries.size.toString(),
                                ),
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    model.nextStep()
                                },
                            ) {
                                Text(stringResource(Res.string.next_step))
                            }
                        },
                        skipButton = {
                            TextButton(
                                onClick = {
                                    model.skipAll()
                                },
                            ) {
                                Text(stringResource(Res.string.skip_setup))
                            }
                        },
                        backButton = {
                            OutlinedButton(
                                onClick = {
                                    model.goback()
                                },
                                enabled = state != LANGUAGE,
                            ) {
                                Text(stringResource(Res.string.previous_step))
                            }
                        },
                    ) {
                        WelcomeElementContent(state)
                    }
                }
            }
        }
    }

    @Composable
    private fun WelcomeElementContent(
        state: WelcomeViewState.ConfigureSetting,
    ) {
        val colors = MaterialTheme.colorScheme
        when (state) {
            LANGUAGE -> {
                var locale by remember {
                    mutableStateOf(AppConfig.locale)
                }

                LaunchedEffect(locale) {
                    AppConfig.locale = locale
                    ComposeI18N.locale.value = locale.locale
                }

                Column(Modifier.fillMaxWidth()) {
                    for (i in AppConfig.LanguageSettings.entries) {
                        androidx.compose.material3.ListItem(
                            headlineContent = {
                                Text(stringResource(i.tag))
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = locale == i,
                                    onClick = {
                                        locale = i
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { locale = i }
                        )
                    }
                }
            }
            WELCOME -> {
                val scheme = MaterialTheme.colorScheme
                Text(
                    buildAnnotatedString {
                        append(stringResource(Res.string.welcome_text, BuildConfig.APP_NAME))
                        withLink(
                            colors = colors,
                            link = "https://github.com/kagg886/pixko",
                            display = " Pixko ",
                        )
                        append(stringResource(Res.string.welcome_text_after_pixko))
                        withLink(scheme, "https://t.me/+n_xsrc1Z590xNTY9", "TG交流群")
                    },
                )
            }

            THEME -> {
                Text(
                    stringResource(Res.string.theme_intro),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
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
                        },
                    ) {
                        var colorScheme by LocalColorScheme.current
                        ListItem(
                            leadingContent = {
                                Icon(LightMode, "")
                            },
                            headlineContent = {
                                Text(stringResource(Res.string.day_mode))
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
                                                it.readBytes().decodeToString(),
                                            )["schemes"]!!.jsonObject["light"]!!.jsonObject.let {
                                                j.decodeFromJsonElement<SerializedTheme>(it)
                                            }
                                        }
                                        if (theme.isFailure) {
                                            scope.launch {
                                                snack.showSnackbar(
                                                    getString(
                                                        Res.string.import_theme_fail,
                                                        theme.exceptionOrNull()?.message
                                                            ?: getString(
                                                                Res.string.unknown_error,
                                                            ),
                                                    ),
                                                )
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
                                            },
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, null)
                                        }
                                        return@AnimatedContent
                                    }
                                    IconButton(
                                        onClick = {
                                            launcher.launch()
                                        },
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, null)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically),
                        )
                    }

                    SelectionCard(
                        select = darkMode == AppConfig.DarkMode.Dark,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            darkMode = AppConfig.DarkMode.Dark
                        },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(DarkMode, "")
                            },
                            headlineContent = {
                                Text(stringResource(Res.string.night_mode))
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically),
                        )
                    }

                    SelectionCard(
                        select = darkMode == AppConfig.DarkMode.System,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            darkMode = AppConfig.DarkMode.System
                        },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(SystemSuggest, "")
                            },
                            headlineContent = {
                                Text(stringResource(Res.string.follow_system))
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        buildAnnotatedString {
                            append(stringResource(Res.string.theme_info))
                            withLink(
                                colors = colors,
                                link = stringResource(Res.string.theme_info_url),
                            )
                            append(stringResource(Res.string.theme_info_after_url))
                        },
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            BYPASS -> {
                Text(
                    stringResource(Res.string.bypass_intro),
                )

                var bypassSettings by remember {
                    mutableStateOf(AppConfig.bypassSettings)
                }
                LaunchedEffect(bypassSettings) {
                    AppConfig.bypassSettings = bypassSettings
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                ) {
                    SelectionCard(
                        select = bypassSettings is AppConfig.BypassSetting.None,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            bypassSettings = AppConfig.BypassSetting.None
                        },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Default.Delete, "")
                            },
                            headlineContent = {
                                Text(stringResource(Res.string.no_bypass))
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically),
                        )
                    }

                    SelectionCard(
                        select = bypassSettings is AppConfig.BypassSetting.SNIReplace,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            bypassSettings = AppConfig.BypassSetting.SNIReplace()
                        },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Default.Check, "")
                            },
                            headlineContent = {
                                Text(stringResource(Res.string.use_sni_bypass))
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically),
                        )
                    }

                    SelectionCard(
                        select = bypassSettings is AppConfig.BypassSetting.Proxy,
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                        onClick = {
                            bypassSettings = AppConfig.BypassSetting.Proxy()
                        },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Default.Check, "")
                            },
                            headlineContent = {
                                Text(stringResource(Res.string.use_proxy))
                            },
                            modifier = Modifier.fillMaxHeight().align(Alignment.CenterVertically),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Card(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(Res.string.bypass_note),
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            SHIELD -> {
                Text(
                    stringResource(Res.string.shield_intro),
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
                        Text(stringResource(Res.string.r18_acceptance))
                    },
                    subtitle = {
                        if (likeR18) {
                            Text(stringResource(Res.string.r18_allowed))
                        } else {
                            Text(stringResource(Res.string.r18_blocked))
                        }
                    },
                    modifier = Modifier.zIndex(0f),
                ) {
                    likeR18 = it
                    if (likeR18.not()) {
                        likeR18G = false
                    }
                }

                AnimatedVisibility(
                    visible = likeR18,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    SettingsSwitch(
                        state = likeR18G,
                        title = {
                            Text(stringResource(Res.string.r18g_acceptance))
                        },
                        modifier = Modifier.zIndex(-1f),
                    ) {
                        likeR18G = it
                    }
                }

                SettingsSwitch(
                    state = likeAI,
                    title = {
                        Text(stringResource(Res.string.ai_acceptance))
                    },
                    subtitle = {
                        if (likeAI) {
                            Text(stringResource(Res.string.ai_allowed))
                        } else {
                            Text(stringResource(Res.string.ai_blocked))
                        }
                    },
                ) {
                    likeAI = it
                }
            }

            FINISH -> {
                Text(
                    if (currentPlatform is Platform.Desktop) {
                        stringResource(Res.string.setup_finish_note)
                    } else {
                        stringResource(Res.string.setup_finish_note_simple)
                    },
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
