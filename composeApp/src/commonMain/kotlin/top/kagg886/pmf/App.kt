package top.kagg886.pmf

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.ScreenTransition
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.http.OkHttpStack
import com.github.panpf.sketch.request.ImageData
import com.github.panpf.sketch.request.RequestInterceptor
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.serialization.decodeValueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.java.KoinJavaComponent.inject
import top.kagg886.pixko.module.user.SimpleMeProfile
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.SystemConfig
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.backend.rootPath
import top.kagg886.pmf.ui.component.CheckUpdateDialog
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.route.login.LoginScreenViewModel
import top.kagg886.pmf.ui.route.main.detail.illust.IllustCommentViewModel
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailViewModel
import top.kagg886.pmf.ui.route.main.detail.novel.NovelCommentViewModel
import top.kagg886.pmf.ui.route.main.history.HistoryIllustViewModel
import top.kagg886.pmf.ui.route.main.history.HistoryNovelViewModel
import top.kagg886.pmf.ui.route.main.download.DownloadScreenModel
import top.kagg886.pmf.ui.route.main.download.DownloadScreenSideEffect
import top.kagg886.pmf.ui.route.main.profile.ProfileScreen
import top.kagg886.pmf.ui.route.main.rank.RankScreen
import top.kagg886.pmf.ui.route.main.recommend.RecommendScreen
import top.kagg886.pmf.ui.route.main.recommend.RecommendIllustViewModel
import top.kagg886.pmf.ui.route.main.recommend.RecommendNovelViewModel
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.route.main.search.SearchViewModel
import top.kagg886.pmf.ui.route.main.space.NewestIllustViewModel
import top.kagg886.pmf.ui.route.main.space.SpaceIllustViewModel
import top.kagg886.pmf.ui.route.main.space.SpaceScreen
import top.kagg886.pmf.ui.route.welcome.WelcomeModel
import top.kagg886.pmf.ui.route.welcome.WelcomeScreen
import top.kagg886.pmf.ui.util.UpdateCheckSideEffect
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.util.ignoreSSL
import java.io.File
import kotlin.reflect.KClass

val LocalSnackBarHost = compositionLocalOf<SnackbarHostState> {
    error("not provided")
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Navigator(WelcomeScreen()) {
                CompositionLocalProvider(
                    LocalSnackBarHost provides remember { SnackbarHostState() },
                ) {
                    val s = LocalSnackBarHost.current
                    CheckUpdateDialog()

                    val model by inject<DownloadScreenModel>(clazz = DownloadScreenModel::class.java)
                    model.collectSideEffect {
                        when (it) {
                            is DownloadScreenSideEffect.Toast -> {
                                s.showSnackbar(it.msg)
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

@Composable
expect fun AppScaffold(nav: Navigator, content: @Composable (Modifier) -> Unit)

@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
@Composable
fun ProfileAvatar() {
    val nav = LocalNavigator.currentOrThrow
    val profile = remember {
        SystemConfig.getConfig("pixiv_token").decodeValueOrNull<SimpleMeProfile>("pixiv_user")!!
    }

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
            directory = rootPath.resolve("cache").resolve("image").toOkioPath(),
            maxSize = AppConfig.cacheSize
        )
    )

    components {
        httpStack(
            OkHttpStack(
                OkHttpClient.Builder().apply {
                    ignoreSSL()
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
            )
        )

        addRequestInterceptor(object : RequestInterceptor {
            override val key: String? = null
            override val sortWeight: Int = 0

            override suspend fun intercept(chain: RequestInterceptor.Chain): Result<ImageData> {
                val newRequest = chain.request.newRequest {
                    setHttpHeader("Referer", "https://www.pixiv.net/")
                }
                return chain.proceed(newRequest)
            }

        })
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
                    IllustDetailViewModel()
                }
                single {
                    IllustCommentViewModel()
                }
                single {
                    SearchViewModel()
                }
                single {
                    RecommendNovelViewModel()
                }

                single {
                    NovelCommentViewModel()
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
            },

            //data base
            module(createdAtStart = true) {
                single {
                    getDataBaseBuilder()
                        .fallbackToDestructiveMigrationOnDowngrade(true)
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

val databasePath = rootPath.resolve("app.db")
expect fun getDataBaseBuilder(): RoomDatabase.Builder<AppDatabase>
expect fun shareFile(file: File)

enum class NavigationItem(val title: String, val icon: ImageVector, val screenClass: KClass<out Screen>) {
    RECOMMEND("推荐", Icons.Default.Home, RecommendScreen::class),
    RANK("排行榜", Icons.Default.DateRange, RankScreen::class),
    SPACE("动态", Icons.Default.Star, SpaceScreen::class)
}