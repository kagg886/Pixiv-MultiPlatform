package top.kagg886.pmf

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.ScreenTransition
import co.touchlab.kermit.Severity
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.disk.DiskCache
import coil3.network.ConnectivityChecker
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.serviceLoaderEnabled
import coil3.util.Logger as CoilLogger
import coil3.util.Logger.Level as CoilLogLevel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.Path
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Level.DEBUG
import org.koin.core.logger.Level.ERROR
import org.koin.core.logger.Level.INFO
import org.koin.core.logger.Level.NONE
import org.koin.core.logger.Level.WARNING
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import org.koin.dsl.module
import org.koin.ext.getFullName
import org.koin.mp.KoinPlatform
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.PlatformConfig
import top.kagg886.pmf.backend.PlatformEngine
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.database.getDataBaseBuilder
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.component.dialog.CheckUpdateDialog
import top.kagg886.pmf.ui.route.main.download.DownloadScreenModel
import top.kagg886.pmf.ui.route.main.download.DownloadScreenSideEffect
import top.kagg886.pmf.ui.route.main.history.HistoryIllustViewModel
import top.kagg886.pmf.ui.route.main.history.HistoryNovelViewModel
import top.kagg886.pmf.ui.route.main.profile.ProfileItem
import top.kagg886.pmf.ui.route.main.profile.ProfileScreen
import top.kagg886.pmf.ui.route.main.rank.RankScreen
import top.kagg886.pmf.ui.route.main.recommend.RecommendIllustViewModel
import top.kagg886.pmf.ui.route.main.recommend.RecommendNovelViewModel
import top.kagg886.pmf.ui.route.main.recommend.RecommendScreen
import top.kagg886.pmf.ui.route.main.search.v2.EmptySearchScreen
import top.kagg886.pmf.ui.route.main.space.NewestIllustViewModel
import top.kagg886.pmf.ui.route.main.space.SpaceIllustViewModel
import top.kagg886.pmf.ui.route.main.space.SpaceScreen
import top.kagg886.pmf.ui.route.welcome.WelcomeModel
import top.kagg886.pmf.ui.route.welcome.WelcomeScreen
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.ui.util.rememberSupportPixivNavigateUriHandler
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.util.SerializedTheme
import top.kagg886.pmf.util.UgoiraFetcher
import top.kagg886.pmf.util.initFileLogger
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.util.toColorScheme

val LocalSnackBarHost = compositionLocalOf<SnackbarHostState> {
    error("not provided")
}

val LocalDarkSettings = compositionLocalOf<MutableState<AppConfig.DarkMode>> {
    error("not provided")
}

val LocalColorScheme = compositionLocalOf<MutableState<SerializedTheme?>> {
    error("not provided")
}

val LocalKeyStateFlow = compositionLocalOf<SharedFlow<KeyEvent>> {
    error("not provided")
}

@OptIn(ExperimentalVoyagerApi::class)
@Composable
@Preview
fun App(initScreen: Screen = WelcomeScreen()) {
    val darkModeValue = remember {
        mutableStateOf(AppConfig.darkMode)
    }
    val colorSchemeValue = remember {
        mutableStateOf(AppConfig.colorScheme)
    }
    CompositionLocalProvider(
        LocalDarkSettings provides darkModeValue,
        LocalColorScheme provides colorSchemeValue,
        LocalSnackBarHost provides remember { SnackbarHostState() },
    ) {
        val currentThemeSerialized by LocalColorScheme.current
        val lightTheme = remember(currentThemeSerialized) {
            currentThemeSerialized?.toColorScheme() ?: lightColorScheme()
        }
        val theme = when (darkModeValue.value) {
            AppConfig.DarkMode.System -> if (isSystemInDarkTheme()) darkColorScheme() else lightTheme
            AppConfig.DarkMode.Light -> lightTheme
            AppConfig.DarkMode.Dark -> darkColorScheme()
        }
        PixivMultiPlatformTheme(colorScheme = theme) {
            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                Navigator(initScreen) {
                    CompositionLocalProvider(
                        LocalUriHandler provides rememberSupportPixivNavigateUriHandler(),
                    ) {
                        val s = LocalSnackBarHost.current
                        CheckUpdateDialog()

                        val model = KoinPlatform.getKoin()
                            .get<DownloadScreenModel>(clazz = DownloadScreenModel::class)
                        model.collectSideEffect { toast ->
                            when (toast) {
                                is DownloadScreenSideEffect.Toast -> {
                                    if (toast.jump) {
                                        val actionYes = getString(Res.string.yes)
                                        val result = s.showSnackbar(
                                            object : SnackbarVisuals {
                                                override val actionLabel: String
                                                    get() = actionYes
                                                override val duration: SnackbarDuration
                                                    get() = SnackbarDuration.Long
                                                override val message: String
                                                    get() = toast.msg
                                                override val withDismissAction: Boolean
                                                    get() = true
                                            },
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            it.push(ProfileScreen(PixivConfig.pixiv_user!!, ProfileItem.Download))
                                        }
                                        return@collectSideEffect
                                    }
                                    s.showSnackbar(toast.msg, withDismissAction = true)
                                }
                            }
                        }
                        AppScaffold { modifier ->
                            ScreenTransition(
                                navigator = it,
                                contentAlignment = Alignment.Center,
                                transition = { fadeIn() togetherWith fadeOut() },
                                modifier = modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationItem.composeWithAppBar(content: @Composable () -> Unit) {
    val nav = LocalNavigator.currentOrThrow
    val type = this
    if (useWideScreenMode) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                SearchButton()
                for (entry in NavigationItems) {
                    NavigationRailItem(
                        selected = entry == type,
                        onClick = { if (entry != type) nav.push(entry) },
                        icon = { Icon(imageVector = entry.icon, null) },
                        label = { Text(entry.title) },
                    )
                }
                Spacer(Modifier.weight(1f))
                ProfileAvatar()
            }
            content()
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { ProfileAvatar() },
                    actions = { SearchButton() },
                )
            },
            bottomBar = {
                NavigationBar {
                    for (entry in NavigationItems) {
                        NavigationBarItem(
                            selected = entry == type,
                            onClick = { if (entry != type) nav.push(entry) },
                            icon = { Icon(imageVector = entry.icon, null) },
                            label = { Text(entry.title) },
                        )
                    }
                }
            },
        ) {
            Box(modifier = Modifier.padding(it)) {
                content()
            }
        }
    }
}

@Composable
fun AppScaffold(content: @Composable (Modifier) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        snackbarHost = { SnackbarHost(hostState = LocalSnackBarHost.current) },
        content = { content(Modifier.fillMaxSize().padding(it)) },
    )
}

@Composable
fun ProfileAvatar() {
    val nav = LocalNavigator.currentOrThrow
    val profile = PixivConfig.pixiv_user!!

    IconButton(
        onClick = {
            nav.push(ProfileScreen(profile))
        },
    ) {
        AsyncImage(
            model = profile.profileImageUrls.content,
            contentDescription = null,
        )
    }
}

@Composable
fun SearchButton() {
    val nav = LocalNavigator.currentOrThrow
    IconButton(
        onClick = {
            nav.push(EmptySearchScreen())
        },
    ) {
        Icon(imageVector = Icons.Default.Search, contentDescription = null)
    }
}

@OptIn(ExperimentalCoilApi::class)
fun ImageLoader.Builder.applyCustomConfig() = apply {
    logger(
        object : CoilLogger {
            override var minLevel = CoilLogLevel.Info
            override fun log(tag: String, level: CoilLogLevel, message: String?, throwable: Throwable?) {
                logger.processLog(
                    severity = when (level) {
                        CoilLogLevel.Verbose -> Severity.Verbose
                        CoilLogLevel.Debug -> Severity.Debug
                        CoilLogLevel.Info -> Severity.Info
                        CoilLogLevel.Warn -> Severity.Warn
                        CoilLogLevel.Error -> Severity.Error
                    },
                    tag = tag,
                    throwable = throwable,
                    message = message.orEmpty(),
                )
            }
        },
    )
    interceptorCoroutineContext(Dispatchers.Default)
    components {
        serviceLoaderEnabled(false)
        add(
            KtorNetworkFetcherFactory(
                httpClient = { KoinPlatform.getKoin().get<HttpClient>() },
                connectivityChecker = { ConnectivityChecker.ONLINE },
            ),
        )
        add(UgoiraFetcher.Factory { KoinPlatform.getKoin().get<HttpClient>() })
        installGifDecoder()
    }
    crossfade(500)
    diskCache {
        DiskCache.Builder().apply {
            directory(cachePath / "coil_image_cache")
            maxSizeBytes(AppConfig.cacheSize)
        }.build()
    }
}

fun setupEnv() {
    // init logger
    initFileLogger()

    // init koin
    startKoin {
        logger(
            object : Logger() {
                override fun display(level: Level, msg: MESSAGE) {
                    if (level == NONE) {
                        return
                    }
                    logger.processLog(
                        severity = when (level) {
                            DEBUG -> Severity.Debug
                            INFO -> Severity.Info
                            WARNING -> Severity.Warn
                            ERROR -> Severity.Error
                            else -> throw IllegalArgumentException("unknown level")
                        },
                        tag = Koin::class.getFullName(),
                        throwable = null,
                        message = msg,
                    )
                }
            },
        )
        modules(
            // vm
            module {
                single {
                    WelcomeModel()
                }
                single {
                    RecommendIllustViewModel()
                }
                single {
                    RecommendNovelViewModel()
                }

                single {
                    SpaceIllustViewModel()
                }

                single {
                    HistoryIllustViewModel()
                }

                single {
                    HistoryNovelViewModel()
                }

                single {
                    NewestIllustViewModel()
                }
            },
            // pixiv
            module(createdAtStart = true) {
                single { PixivTokenStorage() }
                single {
                    HttpClient(PlatformEngine) {
                        PlatformConfig()

                        defaultRequest {
                            header("Referer", "https://www.pixiv.net/")
                        }

                        install(ContentNegotiation) {
                            json(
                                Json {
                                    ignoreUnknownKeys = true
                                },
                            )
                        }
                    }
                }
            },

            // data base
            module(createdAtStart = true) {
                single {
                    getDataBaseBuilder()
                        .fallbackToDestructiveMigrationOnDowngrade(true)
                        .fallbackToDestructiveMigration(true)
                        .fallbackToDestructiveMigrationFrom(true, 1)
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .build()
                }
                single {
                    DownloadScreenModel()
                }
                single {
                    UpdateCheckViewModel()
                }
            },
        )
    }
}

expect fun openBrowser(link: String)

/**
 * # 分享文件。
 *
 * - 在电脑端的实现为直接打开对应的文件
 * - 在安卓端的实现为复制到专用的分享文件夹(cachePath.resolve("share"))后进行发送
 * - 在IOS端会抛出异常
 */
expect fun shareFile(file: Path, name: String = file.name, mime: String = "*/*")

/**
 * # 复制图片到剪切板。
 *
 * - 在电脑端的实现为AWT API
 * - 在安卓端和IOS端会抛出异常
 */
expect suspend fun copyImageToClipboard(bitmap: ByteArray)

val NavigationItems = listOf(NavigationItem.RecommendScreen, NavigationItem.RankScreen, NavigationItem.SpaceScreen)

sealed class NavigationItem(val title: String, val icon: ImageVector, val content: @Composable Screen.() -> Unit) : Screen {
    @Composable
    override fun Content() = composeWithAppBar { content() }

    object RecommendScreen : NavigationItem(runBlocking { getString(Res.string.recommend) }, Icons.Default.Home, { RecommendScreen() })
    object RankScreen : NavigationItem(runBlocking { getString(Res.string.rank) }, Icons.Default.DateRange, { RankScreen() })
    object SpaceScreen : NavigationItem(runBlocking { getString(Res.string.space) }, Icons.Default.Star, { SpaceScreen() })
}

expect fun ComponentRegistry.Builder.installGifDecoder(): ComponentRegistry.Builder
