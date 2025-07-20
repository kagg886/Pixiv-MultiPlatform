package top.kagg886.pmf.ui.route.main.setting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
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
import top.kagg886.pmf.Res
import top.kagg886.pmf.about
import top.kagg886.pmf.advanced
import top.kagg886.pmf.app_language
import top.kagg886.pmf.app_language_feature_pr
import top.kagg886.pmf.appearance
import top.kagg886.pmf.auto_typography
import top.kagg886.pmf.auto_typography_description
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.bypass_none
import top.kagg886.pmf.bypass_proxy
import top.kagg886.pmf.bypass_sni
import top.kagg886.pmf.bypass_solution
import top.kagg886.pmf.bypass_solution_description
import top.kagg886.pmf.cache_size_limit
import top.kagg886.pmf.calculate_by_width
import top.kagg886.pmf.cancel
import top.kagg886.pmf.check_update
import top.kagg886.pmf.check_update_on_start
import top.kagg886.pmf.clear_log_archive
import top.kagg886.pmf.clear_log_archive_description
import top.kagg886.pmf.click_to_modify
import top.kagg886.pmf.column_calculation
import top.kagg886.pmf.column_calculation_description
import top.kagg886.pmf.confirm
import top.kagg886.pmf.confirm_logout
import top.kagg886.pmf.confirm_logout_description
import top.kagg886.pmf.coroutine_exception
import top.kagg886.pmf.coroutine_exception_description
import top.kagg886.pmf.current_mode
import top.kagg886.pmf.current_value
import top.kagg886.pmf.dark_mode
import top.kagg886.pmf.display_mode
import top.kagg886.pmf.doh_address
import top.kagg886.pmf.doh_address_description
import top.kagg886.pmf.doh_address_hint
import top.kagg886.pmf.doh_timeout
import top.kagg886.pmf.doh_timeout_description
import top.kagg886.pmf.doh_timeout_hint
import top.kagg886.pmf.download_theme
import top.kagg886.pmf.export_all_logs
import top.kagg886.pmf.export_all_logs_description
import top.kagg886.pmf.export_login_session
import top.kagg886.pmf.export_login_session_description
import top.kagg886.pmf.export_single_log
import top.kagg886.pmf.export_single_log_description
import top.kagg886.pmf.filter_ai_illustrations
import top.kagg886.pmf.filter_ai_novel
import top.kagg886.pmf.filter_ai_server_hint
import top.kagg886.pmf.filter_long_tag
import top.kagg886.pmf.filter_long_tag_description
import top.kagg886.pmf.filter_r18_benefit
import top.kagg886.pmf.filter_r18_description
import top.kagg886.pmf.filter_r18_illustrations
import top.kagg886.pmf.filter_r18_novel
import top.kagg886.pmf.filter_r18g_description
import top.kagg886.pmf.filter_r18g_enabled_condition
import top.kagg886.pmf.filter_r18g_illustrations
import top.kagg886.pmf.filter_r18g_novel
import top.kagg886.pmf.filter_short_novel
import top.kagg886.pmf.filter_short_novel_description
import top.kagg886.pmf.fixed_column_count
import top.kagg886.pmf.follow_system
import top.kagg886.pmf.format_error
import top.kagg886.pmf.gallery_cache_size
import top.kagg886.pmf.gallery_cache_size_description
import top.kagg886.pmf.gallery_column_count
import top.kagg886.pmf.gallery_column_count_description
import top.kagg886.pmf.gallery_settings
import top.kagg886.pmf.history_records
import top.kagg886.pmf.ignore_ssl_errors
import top.kagg886.pmf.ignore_ssl_errors_description
import top.kagg886.pmf.illust_details_show_all
import top.kagg886.pmf.import_theme_fail
import top.kagg886.pmf.ip_pool
import top.kagg886.pmf.ip_pool_description
import top.kagg886.pmf.ip_pool_hint
import top.kagg886.pmf.light_mode
import top.kagg886.pmf.login_session_copied
import top.kagg886.pmf.login_sessions
import top.kagg886.pmf.logout
import top.kagg886.pmf.logout_description
import top.kagg886.pmf.main_thread_exception
import top.kagg886.pmf.main_thread_exception_description
import top.kagg886.pmf.network_settings
import top.kagg886.pmf.night_mode_not_supported
import top.kagg886.pmf.novel_filter_length
import top.kagg886.pmf.novel_filter_length_description
import top.kagg886.pmf.novel_see_next
import top.kagg886.pmf.novel_see_next_description
import top.kagg886.pmf.novel_settings
import top.kagg886.pmf.proxy_address
import top.kagg886.pmf.proxy_port
import top.kagg886.pmf.proxy_type
import top.kagg886.pmf.record_illustration_history
import top.kagg886.pmf.record_illustration_history_off
import top.kagg886.pmf.record_novel_history
import top.kagg886.pmf.record_novel_history_off
import top.kagg886.pmf.record_search_history
import top.kagg886.pmf.record_search_history_off
import top.kagg886.pmf.reset_theme
import top.kagg886.pmf.reset_theme_description
import top.kagg886.pmf.set_theme
import top.kagg886.pmf.settings
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.show_toast_when_failed
import top.kagg886.pmf.show_toast_when_latest
import top.kagg886.pmf.single_column_width
import top.kagg886.pmf.single_column_width_description
import top.kagg886.pmf.tag_max_length
import top.kagg886.pmf.tag_max_length_description
import top.kagg886.pmf.text_size
import top.kagg886.pmf.theme_builder_hint
import top.kagg886.pmf.theme_builder_url
import top.kagg886.pmf.theme_json_hint
import top.kagg886.pmf.ui.component.settings.SettingsDropdownMenu
import top.kagg886.pmf.ui.component.settings.SettingsFileUpload
import top.kagg886.pmf.ui.component.settings.SettingsTextField
import top.kagg886.pmf.ui.route.login.v2.LoginScreen
import top.kagg886.pmf.ui.route.main.about.AboutScreen
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.update
import top.kagg886.pmf.util.ComposeI18N
import top.kagg886.pmf.util.SerializedTheme
import top.kagg886.pmf.util.b
import top.kagg886.pmf.util.deleteRecursively
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.mb
import top.kagg886.pmf.util.setText
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.util.zip

class SettingScreen : Screen {
    @Composable
    override fun Content() {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (useWideScreenMode) {
                TopAppBar(
                    title = {
                        Text(stringResource(Res.string.settings))
                    },
                )
            }
            SettingsGroup(title = { Text(stringResource(Res.string.appearance)) }) {
                var locale by remember {
                    mutableStateOf(AppConfig.locale)
                }

                LaunchedEffect(locale) {
                    AppConfig.locale = locale
                    ComposeI18N.locale.value = locale.locale
                }

                SettingsDropdownMenu<AppConfig.LanguageSettings>(
                    title = { Text(stringResource(Res.string.app_language)) },
                    subTitle = {
                        Text(stringResource(Res.string.app_language_feature_pr))
                    },
                    optionsFormat = {
                        stringResource(it.tag)
                    },
                    current = locale,
                    data = AppConfig.LanguageSettings.entries,
                    onSelected = {
                        locale = it
                    },
                )

                var darkMode by LocalDarkSettings.current

                LaunchedEffect(darkMode) {
                    AppConfig.darkMode = darkMode
                }

                SettingsDropdownMenu<AppConfig.DarkMode>(
                    title = { Text(stringResource(Res.string.display_mode)) },
                    subTitle = {
                        Text(
                            stringResource(
                                Res.string.current_mode,
                                when (darkMode) {
                                    AppConfig.DarkMode.System -> stringResource(Res.string.follow_system)
                                    AppConfig.DarkMode.Light -> stringResource(Res.string.light_mode)
                                    AppConfig.DarkMode.Dark -> stringResource(Res.string.dark_mode)
                                },
                            ),
                        )
                    },
                    optionsFormat = {
                        when (it) {
                            AppConfig.DarkMode.System -> stringResource(Res.string.follow_system)
                            AppConfig.DarkMode.Light -> stringResource(Res.string.light_mode)
                            AppConfig.DarkMode.Dark -> stringResource(Res.string.dark_mode)
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
                    title = { Text(stringResource(Res.string.set_theme)) },
                    enabled = !inNight,
                    extensions = listOf("zip", "json"),
                    subTitle = {
                        AnimatedContent(
                            inNight,
                        ) {
                            if (it) {
                                Text(stringResource(Res.string.night_mode_not_supported))
                                return@AnimatedContent
                            }
                            val colors = MaterialTheme.colorScheme
                            Text(
                                buildAnnotatedString {
                                    append(stringResource(Res.string.theme_builder_hint))
                                    withLink(
                                        LinkAnnotation.Url(
                                            url = stringResource(Res.string.theme_builder_url),
                                            styles = TextLinkStyles(
                                                style = SpanStyle(color = colors.primary),
                                                hoveredStyle = SpanStyle(
                                                    color = colors.primaryContainer,
                                                    textDecoration = TextDecoration.Underline,
                                                ),
                                            ),
                                        ),
                                    ) {
                                        append(stringResource(Res.string.theme_builder_url))
                                    }
                                    appendLine(stringResource(Res.string.download_theme))
                                    append(stringResource(Res.string.theme_json_hint))
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
                            snack.showSnackbar(getString(Res.string.import_theme_fail, theme.exceptionOrNull()!!.message.toString()))
                        }
                        return@SettingsFileUpload
                    }
                    colorScheme = theme.getOrThrow()
                }

                SettingsMenuLink(
                    title = { Text(stringResource(Res.string.reset_theme)) },
                    subtitle = { Text(stringResource(Res.string.reset_theme_description)) },
                    onClick = { colorScheme = null },
                    enabled = colorScheme != null,
                )
            }
            SettingsGroup(title = { Text(stringResource(Res.string.gallery_settings)) }) {
                var data by remember { mutableStateOf(AppConfig.galleryOptions) }
                LaunchedEffect(data) {
                    AppConfig.galleryOptions = data
                }
                SettingsDropdownMenu(
                    title = { Text(stringResource(Res.string.column_calculation)) },
                    subTitle = { Text(stringResource(Res.string.column_calculation_description)) },
                    optionsFormat = {
                        when (it) {
                            is AppConfig.Gallery.FixColumnCount -> stringResource(Res.string.fixed_column_count)
                            is AppConfig.Gallery.FixWidth -> stringResource(Res.string.calculate_by_width)
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
                                    Text(stringResource(Res.string.gallery_column_count))
                                },
                                subtitle = {
                                    Text(stringResource(Res.string.gallery_column_count_description, defaultGalleryWidth))
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
                                    Text(stringResource(Res.string.single_column_width))
                                },
                                subtitle = {
                                    Text(stringResource(Res.string.single_column_width_description, defaultGalleryWidth))
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
                        Text(stringResource(Res.string.gallery_cache_size))
                    },
                    subTitle = {
                        Column {
                            Text(stringResource(Res.string.gallery_cache_size_description))
                            Text(stringResource(Res.string.current_value, cacheSize.b))
                            Text(stringResource(Res.string.click_to_modify))
                        }
                    },
                    dialogLabel = {
                        Text(stringResource(Res.string.cache_size_limit))
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
                        Text(stringResource(Res.string.filter_ai_illustrations))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.filter_ai_server_hint))
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
                        Text(stringResource(Res.string.filter_r18_illustrations))
                    },
                    subtitle = {
                        Column {
                            Text(stringResource(Res.string.filter_r18_description))
                            Text(stringResource(Res.string.filter_r18_benefit))
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
                        Text(stringResource(Res.string.filter_r18g_illustrations))
                    },
                    subtitle = {
                        Column {
                            Text(stringResource(Res.string.filter_r18g_description))
                            Text(stringResource(Res.string.filter_r18g_enabled_condition))
                        }
                    },
                    onCheckedChange = {
                        filterR18G = it
                    },
                )
                var illustDetailsShowAll by remember { mutableStateOf(AppConfig.illustDetailsShowAll) }
                LaunchedEffect(illustDetailsShowAll) {
                    AppConfig.illustDetailsShowAll = illustDetailsShowAll
                }
                SettingsSwitch(
                    state = illustDetailsShowAll,
                    title = { Text(stringResource(Res.string.illust_details_show_all)) },
                    onCheckedChange = { illustDetailsShowAll = it },
                )
            }
            SettingsGroup(title = { Text(stringResource(Res.string.novel_settings)) }) {
                var filterAiNovel by remember {
                    mutableStateOf(AppConfig.filterAiNovel)
                }
                LaunchedEffect(filterAiNovel) {
                    AppConfig.filterAiNovel = filterAiNovel
                }
                SettingsSwitch(
                    state = filterAiNovel,
                    title = {
                        Text(stringResource(Res.string.filter_ai_novel))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.filter_ai_server_hint))
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
                        Text(stringResource(Res.string.filter_r18_novel))
                    },
                    subtitle = {
                        Column {
                            Text(stringResource(Res.string.filter_r18_description))
                            Text(stringResource(Res.string.filter_r18_benefit))
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
                        Text(stringResource(Res.string.filter_r18g_novel))
                    },
                    subtitle = {
                        Column {
                            Text(stringResource(Res.string.filter_r18g_description))
                            Text(stringResource(Res.string.filter_r18g_enabled_condition))
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
                        Text(stringResource(Res.string.auto_typography))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.auto_typography_description, textSize * 2))
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
                        Text(stringResource(Res.string.text_size))
                    },
                    subtitle = {
                        Text("${textSize}sp", fontSize = textSize.sp)
                    },
                )

                var enableFetchSeries by remember {
                    mutableStateOf(AppConfig.enableFetchSeries)
                }
                LaunchedEffect(enableFetchSeries) {
                    AppConfig.enableFetchSeries = enableFetchSeries
                }
                SettingsSwitch(
                    state = enableFetchSeries,
                    title = {
                        Text(stringResource(Res.string.novel_see_next))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.novel_see_next_description))
                    },
                    onCheckedChange = {
                        enableFetchSeries = it
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
                        Text(stringResource(Res.string.filter_long_tag))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.filter_long_tag_description))
                    },
                    onCheckedChange = {
                        filterLongTag = it
                    },
                )
                SettingsSlider(
                    enabled = filterLongTag,
                    title = {
                        Text(stringResource(Res.string.tag_max_length))
                    },
                    subtitle = {
                        Column {
                            Text(stringResource(Res.string.tag_max_length_description))
                            Text(stringResource(Res.string.current_value, filterLongTagLength))
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
                        Text(stringResource(Res.string.filter_short_novel))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.filter_short_novel_description))
                    },
                    onCheckedChange = {
                        filterShortNovel = it
                    },
                )
                SettingsSlider(
                    enabled = filterShortNovel,
                    title = {
                        Text(stringResource(Res.string.novel_filter_length))
                    },
                    subtitle = {
                        Column {
                            Text(stringResource(Res.string.novel_filter_length_description))
                            Text(stringResource(Res.string.current_value, filterShortNovelLength))
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
            SettingsGroup(title = { Text(stringResource(Res.string.history_records)) }) {
                var recordIllustHistory by remember {
                    mutableStateOf(AppConfig.recordIllustHistory)
                }
                LaunchedEffect(recordIllustHistory) {
                    AppConfig.recordIllustHistory = recordIllustHistory
                }
                SettingsSwitch(
                    state = recordIllustHistory,
                    title = {
                        Text(stringResource(Res.string.record_illustration_history))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.record_illustration_history_off))
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
                        Text(stringResource(Res.string.record_novel_history))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.record_novel_history_off))
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
                        Text(stringResource(Res.string.record_search_history))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.record_search_history_off))
                    },
                    onCheckedChange = {
                        recordSearchHistory = it
                    },
                )
            }
            SettingsGroup(title = { Text(stringResource(Res.string.network_settings)) }) {
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
                    title = { Text(stringResource(Res.string.bypass_solution)) },
                    subTitle = { Text(stringResource(Res.string.bypass_solution_description)) },
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
                            AppConfig.BypassSetting.None -> stringResource(Res.string.bypass_none)
                            is AppConfig.BypassSetting.Proxy -> stringResource(Res.string.bypass_proxy)
                            is AppConfig.BypassSetting.SNIReplace -> stringResource(Res.string.bypass_sni)
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
                                    title = { Text(stringResource(Res.string.proxy_type)) },
                                    current = readOnlySettings.method,
                                    data = AppConfig.BypassSetting.Proxy.ProxyType.entries,
                                    onSelected = {
                                        bypassSetting = readOnlySettings.copy(method = it)
                                    },
                                )

                                SettingsTextField(
                                    title = { Text(stringResource(Res.string.proxy_address)) },
                                    value = readOnlySettings.host,
                                    onValueChange = {
                                        bypassSetting = readOnlySettings.copy(host = it)
                                    },
                                )
                                SettingsTextField(
                                    title = { Text(stringResource(Res.string.proxy_port)) },
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
                                    title = { Text(stringResource(Res.string.doh_address)) },
                                    subTitle = {
                                        Text(
                                            buildAnnotatedString {
                                                appendLine(stringResource(Res.string.doh_address_description))
                                                appendLine(stringResource(Res.string.doh_address_hint))
                                            },
                                        )
                                    },
                                    value = readOnlySettings.url,
                                    onValueChange = {
                                        bypassSetting = readOnlySettings.copy(url = it)
                                    },
                                )

                                SettingsTextField(
                                    title = { Text(stringResource(Res.string.doh_timeout)) },
                                    subTitle = {
                                        Text(
                                            buildAnnotatedString {
                                                appendLine(stringResource(Res.string.doh_timeout_description))
                                                appendLine(stringResource(Res.string.doh_timeout_hint))
                                            },
                                        )
                                    },
                                    value = readOnlySettings.dohTimeout.toString(),
                                    onValueChange = {
                                        val data = it.toIntOrNull() ?: run {
                                            scope.launch {
                                                snack.showSnackbar(getString(Res.string.format_error))
                                            }
                                            return@run 5
                                        }
                                        bypassSetting = readOnlySettings.copy(dohTimeout = data)
                                    },
                                )
                                SettingsSwitch(
                                    state = readOnlySettings.nonStrictSSL,
                                    title = {
                                        Text(stringResource(Res.string.ignore_ssl_errors))
                                    },
                                    subtitle = {
                                        Text(stringResource(Res.string.ignore_ssl_errors_description))
                                    },
                                    onCheckedChange = {
                                        bypassSetting = readOnlySettings.copy(nonStrictSSL = it)
                                    },
                                )
                                SettingsTextField(
                                    title = { Text(stringResource(Res.string.ip_pool)) },
                                    subTitle = {
                                        Text(
                                            buildAnnotatedString {
                                                appendLine(stringResource(Res.string.ip_pool_description))
                                                appendLine(stringResource(Res.string.ip_pool_hint))
                                            },
                                        )
                                    },
                                    value = Json.encodeToString(readOnlySettings.fallback),
                                    onValueChange = {
                                        val fallback = try {
                                            Json.decodeFromString<Map<String, List<String>>>(it)
                                        } catch (_: Exception) {
                                            scope.launch {
                                                snack.showSnackbar(getString(Res.string.format_error))
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
            SettingsGroup(title = { Text(stringResource(Res.string.login_sessions)) }) {
                val clip = LocalClipboard.current
                val scope = rememberCoroutineScope()
                val snack = LocalSnackBarHost.current
                SettingsMenuLink(
                    title = {
                        Text(stringResource(Res.string.export_login_session))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.export_login_session_description))
                    },
                    onClick = {
                        scope.launch {
                            clip.setText(
                                PixivConfig.refreshToken,
                            )
                            snack.showSnackbar(getString(Res.string.login_session_copied))
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
                            Text(stringResource(Res.string.confirm_logout))
                        },
                        text = {
                            Text(stringResource(Res.string.confirm_logout_description))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    nav.replaceAll(LoginScreen(true))
                                },
                            ) {
                                Text(stringResource(Res.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    show = false
                                },
                            ) {
                                Text(stringResource(Res.string.cancel))
                            }
                        },
                    )
                }
                SettingsMenuLink(
                    title = {
                        Text(stringResource(Res.string.logout))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.logout_description))
                    },
                    onClick = {
                        show = true
                    },
                )
            }
            SettingsGroup(title = { Text(stringResource(Res.string.advanced)) }) {
                SettingsMenuLink(
                    title = {
                        Text(stringResource(Res.string.main_thread_exception))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.main_thread_exception_description))
                    },
                    onClick = {
                        throw RuntimeException("测试异常，请不要反馈。")
                    },
                )
                val scope = rememberCoroutineScope()
                SettingsMenuLink(
                    title = {
                        Text(stringResource(Res.string.coroutine_exception))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.coroutine_exception_description))
                    },
                    onClick = {
                        scope.launch {
                            throw RuntimeException("测试异常，请不要反馈。")
                        }
                    },
                )

                SettingsMenuLink(
                    title = {
                        Text(stringResource(Res.string.export_single_log))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.export_single_log_description))
                    },
                    onClick = {
                        scope.launch {
                            shareFile(cachePath.resolve("log").resolve("latest.log"), mime = MimeType.TEXT_PLAIN.mime)
                        }
                    },
                )
                SettingsMenuLink(
                    title = {
                        Text(stringResource(Res.string.clear_log_archive))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.clear_log_archive_description))
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
                        Text(stringResource(Res.string.export_all_logs))
                    },
                    subtitle = {
                        Text(stringResource(Res.string.export_all_logs_description))
                    },
                    onClick = {
                        scope.launch {
                            shareFile(cachePath.resolve("log").zip())
                        }
                    },
                )
            }
            SettingsGroup(title = { Text(stringResource(Res.string.update)) }) {
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
                        Text(stringResource(Res.string.check_update_on_start))
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
                            Text(stringResource(Res.string.show_toast_when_latest))
                        },
                        onCheckedChange = {
                            showCheckSuccessToast = it
                        },
                    )

                    SettingsSwitch(
                        enabled = checkUpdateOnStart,
                        state = showCheckFailedToast,
                        title = {
                            Text(stringResource(Res.string.show_toast_when_failed))
                        },
                        onCheckedChange = {
                            showCheckFailedToast = it
                        },
                    )
                }

                SettingsMenuLink(
                    title = {
                        Text(stringResource(Res.string.check_update))
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
                        Text(stringResource(Res.string.about))
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
