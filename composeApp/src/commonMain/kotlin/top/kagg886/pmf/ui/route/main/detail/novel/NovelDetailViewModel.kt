package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size as CoilSize
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Entities
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import kotlin.collections.set
import kotlin.time.Clock
import kotlin.use
import kotlin.uuid.Uuid
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer
import okio.buffer
import okio.use
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.epub.builder.EpubBuilder
import top.kagg886.epub.data.ResourceItem
import top.kagg886.filepicker.FilePicker
import top.kagg886.filepicker.openFileSaver
import top.kagg886.pixko.Tag
import top.kagg886.pixko.anno.ExperimentalNovelParserAPI
import top.kagg886.pixko.module.illust.BookmarkVisibility
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.NovelData
import top.kagg886.pixko.module.novel.NovelImagesSize
import top.kagg886.pixko.module.novel.SeriesInfo
import top.kagg886.pixko.module.novel.bookmarkNovel
import top.kagg886.pixko.module.novel.deleteBookmarkNovel
import top.kagg886.pixko.module.novel.getNovelContent
import top.kagg886.pixko.module.novel.getNovelDetail
import top.kagg886.pixko.module.novel.getNovelSeries
import top.kagg886.pixko.module.novel.parser.v2.CombinedText
import top.kagg886.pixko.module.novel.parser.v2.JumpPageNode
import top.kagg886.pixko.module.novel.parser.v2.JumpUriNode
import top.kagg886.pixko.module.novel.parser.v2.NewPageNode
import top.kagg886.pixko.module.novel.parser.v2.PixivImageNode
import top.kagg886.pixko.module.novel.parser.v2.TextNode
import top.kagg886.pixko.module.novel.parser.v2.TitleNode
import top.kagg886.pixko.module.novel.parser.v2.UploadImageNode
import top.kagg886.pixko.module.novel.parser.v2.content
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.NovelHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.util.NovelNodeElement
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.delete
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.util.source

class NovelDetailViewModel(
    val id: Long,
    val seriesInfo: SeriesInfo? = null,
) : ViewModel(),
    ScreenModel,
    ContainerHost<NovelDetailViewState, NovelDetailSideEffect>,
    KoinComponent {
    override val container: Container<NovelDetailViewState, NovelDetailSideEffect> =
        container(NovelDetailViewState.Loading(MutableStateFlow("Loading...")))
    private val client = PixivConfig.newAccountFromConfig()
    private val img by inject<HttpClient>()
    private val database by inject<AppDatabase>()

    private fun CombinedText.toPlainString() = this.joinToString("") { it.text }

    @OptIn(ExperimentalNovelParserAPI::class)
    fun reload(coil: PlatformContext) = intent {
        val loading =
            NovelDetailViewState.Loading(MutableStateFlow(getString(Res.string.get_novel_detail)))
        reduce { loading }

        val result = kotlin.runCatching {
            client.getNovelDetail(id) to client.getNovelContent(id)
        }
        if (result.isFailure) {
            logger.e("get novel info failed:", result.exceptionOrNull())
            val err = getString(Res.string.load_failed)
            reduce { NovelDetailViewState.Error(err) }
            return@intent
        }
        val (detail, content) = result.getOrThrow()
        val images = kotlin.runCatching { content.images }.getOrElse { emptyMap() }

        val nodeMap = linkedMapOf<Int, NovelNodeElement>()

        val data = kotlin.runCatching {
            content.content.value
        }

        if (data.isFailure) {
            val err = getString(Res.string.load_failed)
            reduce {
                NovelDetailViewState.Error(err)
            }
            return@intent
        }

        // 异步获取image
        coroutineScope {
            var parsed by atomic(0)
            for ((index, i) in data.getOrThrow().withIndex()) {
                when (i) {
                    is JumpUriNode -> {
                        nodeMap[index] = NovelNodeElement.JumpUri(i.text, i.uri)
                        parsed++
                        loading.text.emit(
                            getString(
                                Res.string.parse_novel_node,
                                parsed,
                                data.getOrThrow().size,
                            ),
                        )
                    }

                    is UploadImageNode -> {
                        val priority = listOf(
                            NovelImagesSize.N480Mw,
                            NovelImagesSize.N1200x1200,
                            NovelImagesSize.N128x128,
                            NovelImagesSize.NOriginal,
                            NovelImagesSize.N240Mw,
                        )
                        val img = priority.firstNotNullOf {
                            kotlin.runCatching {
                                images[i.url]!![it]
                            }.getOrNull()
                        }

                        val resp = SingletonImageLoader.get(coil).execute(
                            ImageRequest.Builder(context = coil).size(CoilSize.ORIGINAL).data(img)
                                .build(),
                        )
                        if (resp is SuccessResult) {
                            val info = resp.image
                            nodeMap[index] = NovelNodeElement.UploadImage(
                                img,
                                Size(info.width.toFloat(), info.height.toFloat()),
                            )

                            parsed++
                            loading.text.emit(
                                getString(
                                    Res.string.parse_novel_node,
                                    parsed,
                                    data.getOrThrow().size,
                                ),
                            )

                            continue
                        }
                        logger.w { "getting novel image: $img failed, msg:$resp" }
                    }

                    is PixivImageNode -> {
                        launch {
                            val illust = client.getIllustDetail(i.id.toLong())
                            nodeMap[index] = NovelNodeElement.PixivImage(
                                illust,
                            )
                            parsed++
                            loading.text.emit(
                                getString(
                                    Res.string.parse_novel_node,
                                    parsed,
                                    data.getOrThrow().size,
                                ),
                            )
                        }
                    }

                    is NewPageNode -> {
                        nodeMap[index] = NovelNodeElement.NewPage(index + 1)
                        parsed++
                        loading.text.emit(
                            getString(
                                Res.string.parse_novel_node,
                                parsed,
                                data.getOrThrow().size,
                            ),
                        )
                    }

                    is TextNode -> {
                        nodeMap[index] = NovelNodeElement.Plain(i.text.toPlainString())
                        parsed++
                        loading.text.emit(
                            getString(
                                Res.string.parse_novel_node,
                                parsed,
                                data.getOrThrow().size,
                            ),
                        )
                    }

                    is TitleNode -> {
                        nodeMap[index] = NovelNodeElement.Title(i.text.toPlainString())
                        parsed++
                        loading.text.emit(
                            getString(
                                Res.string.parse_novel_node,
                                parsed,
                                data.getOrThrow().size,
                            ),
                        )
                    }

                    is JumpPageNode -> {
                        nodeMap[index] = NovelNodeElement.JumpPage(i.page)
                        parsed++
                        loading.text.emit(
                            getString(
                                Res.string.parse_novel_node,
                                parsed,
                                data.getOrThrow().size,
                            ),
                        )
                    }
                }
            }
        }

        // 在传入seriesInfo时使用seriesInfo，否则拉取所有series。
        // 如果为null代表这个小说没有series
        loading.text.emit(getString(Res.string.parsing_novel_series))

        val seriesInfo =
            if (!AppConfig.enableFetchSeries) {
                null
            } else {
                seriesInfo ?: detail.series.id?.let {
                    if (it == -1) return@let null // 默认值为-1
                    val seriesInfo = client.getNovelSeries(it)
                    val mutex = Mutex()
                    var progress = 0
                    val other = coroutineScope {
                        (2..<seriesInfo.novelSeriesDetail.pageCount).map { page ->
                            async {
                                client.getNovelSeries(it, page).novels.apply {
                                    mutex.withLock {
                                        loading.text.emit(
                                            getString(
                                                Res.string.parsing_novel_series_progress,
                                                ++progress,
                                                seriesInfo.novelSeriesDetail.pageCount - 1,
                                            ),
                                        )
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                    SeriesInfo(
                        novelSeriesDetail = seriesInfo.novelSeriesDetail,
                        novels = seriesInfo.novels + other.flatten(),
                    )
                }
            }

        reduce {
            NovelDetailViewState.Success(
                detail,
                content,
                nodeMap.toList().sortedBy { it.first }.map { it.second },
                seriesInfo = seriesInfo,
            )
        }
        if (AppConfig.recordNovelHistory) {
            database.novelHistoryDAO()
                .insert(NovelHistory(id, detail, Clock.System.now().toEpochMilliseconds()))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun likeNovel(
        visibility: BookmarkVisibility = BookmarkVisibility.PUBLIC,
        tags: List<Tag>? = null,
    ) = intent {
        runOn<NovelDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.bookmarkNovel(id) {
                    this.tags = tags
                    this.visibility = visibility
                }
            }

            if (result.isFailure) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.bookmark_failed)))
                return@runOn
            }
            reduce {
                state.copy(
                    novel = state.novel.copy(isBookmarked = true),
                )
            }
            postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.bookmark_success)))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeNovel() = intent {
        runOn<NovelDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.deleteBookmarkNovel(id)
            }

            if (result.isFailure) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.un_bookmark_failed)))
                return@runOn
            }
            reduce {
                state.copy(
                    novel = state.novel.copy(isBookmarked = false),
                )
            }
            postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.un_bookmark_success)))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun followUser(private: Boolean = false) = intent {
        runOn<NovelDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.followUser(
                    state.novel.user.id,
                    publicity = if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC,
                )
            }
            if (result.isFailure) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.follow_fail)))
                return@runOn
            }
            if (private) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.follow_success_private)))
            } else {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.follow_success)))
            }
            reduce {
                state.copy(
                    novel = state.novel.copy(
                        user = state.novel.user.copy(
                            isFollowed = true,
                        ),
                    ),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser() = intent {
        runOn<NovelDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.unFollowUser(state.novel.user.id)
            }
            if (result.isFailure) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.unfollow_fail)))
                return@runOn
            }
            postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.unfollow_success)))
            reduce {
                state.copy(
                    novel = state.novel.copy(
                        user = state.novel.user.copy(
                            isFollowed = false,
                        ),
                    ),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun navigateNextPage() = intent {
        runOn<NovelDetailViewState.Success> {
            val info = state.seriesInfo
            if (info == null) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.cant_jump_series_because_no_series)))
                return@runOn
            }

            val target = info.novels.indexOfFirst { it.id.toLong() == id }

            if (target == -1) {
                throw IllegalStateException("the series get failed")
            }

            if (target == info.novels.size - 1) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.cant_jump_series_because_page_incorrect_in_last)))
                return@runOn
            }

            postSideEffect(
                NovelDetailSideEffect.NavigateToOtherNovel(
                    id = info.novels[target + 1].id.toLong(),
                    seriesInfo = info,
                ),
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun navigatePreviousPage() = intent {
        runOn<NovelDetailViewState.Success> {
            val info = state.seriesInfo
            if (info == null) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.cant_jump_series_because_no_series)))
                return@runOn
            }

            val target = info.novels.indexOfFirst { it.id.toLong() == id }

            if (target == -1) {
                throw IllegalStateException("the series get failed")
            }

            if (target == 0) {
                postSideEffect(NovelDetailSideEffect.Toast(getString(Res.string.cant_jump_series_because_page_incorrect_in_last)))
                return@runOn
            }

            postSideEffect(
                NovelDetailSideEffect.NavigateToOtherNovel(
                    id = info.novels[target - 1].id.toLong(),
                    seriesInfo = info,
                ),
            )
        }
    }
}

sealed class NovelDetailViewState {
    data class Loading(val text: MutableStateFlow<String>) : NovelDetailViewState()
    data class Error(val cause: String) : NovelDetailViewState()
    data class Success(
        val novel: Novel,
        val core: NovelData,
        val nodeMap: List<NovelNodeElement>,
        val seriesInfo: SeriesInfo? = null,
    ) : NovelDetailViewState()
}

sealed class NovelDetailSideEffect {
    data class Toast(val msg: String) : NovelDetailSideEffect()
    data class NavigateToOtherNovel(val id: Long, val seriesInfo: SeriesInfo?) :
        NovelDetailSideEffect()
}
