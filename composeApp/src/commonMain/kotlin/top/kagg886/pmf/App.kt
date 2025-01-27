package top.kagg886.pmf

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.ScreenTransition
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.fetch.supportOkHttpHttpUri
import com.github.panpf.sketch.http.OkHttpStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.java.KoinJavaComponent.inject
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.database.getDataBaseBuilder
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage

import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.component.dialog.CheckUpdateDialog
import top.kagg886.pmf.ui.route.login.LoginScreenViewModel
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
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.route.main.search.SearchViewModel
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
import top.kagg886.pmf.util.bypassSNI
import top.kagg886.pmf.util.toColorScheme
import top.kagg886.pmf.util.toSerialized
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

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

@Composable
@Preview
fun App() {
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
                color = MaterialTheme.colorScheme.background
            ) {
                Navigator(WelcomeScreen()) {
                    CompositionLocalProvider(
                        LocalUriHandler provides rememberSupportPixivNavigateUriHandler(),
                    ) {
                        val s = LocalSnackBarHost.current
                        CheckUpdateDialog()

                        val model by inject<DownloadScreenModel>(clazz = DownloadScreenModel::class.java)
                        model.collectSideEffect { toast ->
                            when (toast) {
                                is DownloadScreenSideEffect.Toast -> {
                                    if (toast.jump) {
                                        val result = s.showSnackbar(
                                            object : SnackbarVisuals {
                                                override val actionLabel: String
                                                    get() = "是"
                                                override val duration: SnackbarDuration
                                                    get() = SnackbarDuration.Long
                                                override val message: String
                                                    get() = toast.msg
                                                override val withDismissAction: Boolean
                                                    get() = true
                                            }
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
                        AppScaffold(it) { modifier ->
                            Box(modifier = modifier) {
                                ScreenTransition(
                                    navigator = it,
                                    transition = {
                                        fadeIn() togetherWith fadeOut()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(nav: Navigator, content: @Composable (Modifier) -> Unit) {
    if (useWideScreenMode) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = LocalSnackBarHost.current)
            }
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(it)) {
                if (NavigationItem.entries.any { item -> item.screenClass.isInstance(nav.lastItemOrNull) }) {
                    NavigationRail {
                        SearchButton()
                        for (entry in NavigationItem.entries) {
                            NavigationRailItem(
                                selected = entry.screenClass.isInstance(nav.lastItemOrNull),
                                onClick = {
                                    if (entry.screenClass.isInstance(nav.lastItemOrNull)) {
                                        return@NavigationRailItem
                                    }
                                    nav.push(entry.screenClass.primaryConstructor!!.call())
                                },
                                icon = {
                                    Icon(imageVector = entry.icon, null)
                                },
                                label = {
                                    Text(entry.title)
                                }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        ProfileAvatar()
                    }
                }
                content(Modifier.fillMaxSize())
            }

        }
        return
    }

    var title by remember {
        mutableStateOf("推荐")
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        snackbarHost = {
            SnackbarHost(
                LocalSnackBarHost.current
            )
        },
        topBar = {
            if (NavigationItem.entries.any { it.screenClass.isInstance(nav.lastItemOrNull) }) {
                TopAppBar(
                    title = {
                        Text(title)
                    },
                    navigationIcon = {
                        ProfileAvatar()
                    },
                    actions = {
                        SearchButton()
                    }
                )
            }
        },
        bottomBar = {
            if (NavigationItem.entries.any { it.screenClass.isInstance(nav.lastItemOrNull) }) {
                NavigationBar {
                    for (entry in NavigationItem.entries) {
                        NavigationBarItem(
                            selected = entry.screenClass.isInstance(nav.lastItemOrNull),
                            onClick = {
                                if (entry.screenClass.isInstance(nav.lastItemOrNull)) {
                                    return@NavigationBarItem
                                }
                                title = entry.title
                                nav.push(entry.screenClass.primaryConstructor!!.call())
                            },
                            icon = {
                                Icon(imageVector = entry.icon, null)
                            },
                            label = {
                                Text(entry.title)
                            }
                        )
                    }
                }
            }
        }
    ) {
        content(Modifier.padding(it))
    }


}

@Composable
fun ProfileAvatar() {
    val nav = LocalNavigator.currentOrThrow
    val profile = PixivConfig.pixiv_user!!

    IconButton(
        onClick = {
            nav.push(ProfileScreen(profile))
        }
    ) {
        ProgressedAsyncImage(
            url = profile.profileImageUrls.content
        )
    }
}

@Composable
fun SearchButton() {
    val nav = LocalNavigator.currentOrThrow
    IconButton(
        onClick = {
            nav.push(SearchScreen())
        }
    ) {
        Icon(imageVector = Icons.Default.Search, contentDescription = null)
    }
}


fun Sketch.Builder.applyCustomSketchConfig(): Sketch {
    resultCacheOptions(
        DiskCache.Options(
            directory = cachePath.resolve("image").toOkioPath(),
            maxSize = AppConfig.cacheSize
        )
    )

    components {
        val okhttp = getKoin().get<OkHttpClient>()
        supportOkHttpHttpUri(OkHttpStack(okhttp))
    }
    return build()
}

fun startKoin0() {
    startKoin {
        modules(
            //vm
            module {
                single {
                    LoginScreenViewModel()
                }
                single {
                    WelcomeModel()
                }
                single {
                    RecommendIllustViewModel()
                }
                single {
                    SearchViewModel()
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
            //pixiv
            module {
                single { PixivTokenStorage() }
                single {
                    OkHttpClient.Builder().apply {
                        if (AppConfig.byPassSNI) {
                            bypassSNI()
                        }
                        addInterceptor {
                            val request = it.request().newBuilder()
//                            with(AppConfig.customPixivImageHost) {
//                                if (isNotBlank()) {
//                                    val originUrl = it.request().url
//                                    request.url(
//                                        this.toHttpUrl().newBuilder()
//                                            .encodedPath(originUrl.encodedPath)
//                                            .encodedQuery(originUrl.encodedQuery)
//                                            .build()
//                                    )
//                                }
//                            }
                            request.header("Referer", "https://www.pixiv.net/")
                            it.proceed(request.build())
                        }
                        callTimeout(30, TimeUnit.SECONDS)
                        connectTimeout(30, TimeUnit.SECONDS)
                        readTimeout(30, TimeUnit.SECONDS)
                        writeTimeout(30, TimeUnit.SECONDS)
                        addNetworkInterceptor {
                            val resp = it.proceed(it.request())
                            //防止connection leak
                            val content = resp.body?.byteStream()?.use { stream ->
                                stream.readBytes()
                            }
                            resp.newBuilder()
                                .body(content?.toResponseBody(resp.body?.contentType()))
                                .build()
                        }
                    }.build()
                }
            },

            //data base
            module(createdAtStart = true) {
                single {
                    getDataBaseBuilder()
                        .fallbackToDestructiveMigrationOnDowngrade(true)
                        .fallbackToDestructiveMigration(true)
                        .fallbackToDestructiveMigrationFrom(true, 1)
                        .setDriver(BundledSQLiteDriver())
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .build()
                }
                single {
                    DownloadScreenModel()
                }
                single {
                    UpdateCheckViewModel()
                }
            }
        )
    }
}

expect fun shareFile(file: File, name: String = file.name, mime: String = "*/*")
expect suspend fun copyImageToClipboard(bitmap: ByteArray)

enum class NavigationItem(val title: String, val icon: ImageVector, val screenClass: KClass<out Screen>) {
    RECOMMEND("推荐", Icons.Default.Home, RecommendScreen::class),
    RANK("排行榜", Icons.Default.DateRange, RankScreen::class),
    SPACE("动态", Icons.Default.Star, SpaceScreen::class)
}