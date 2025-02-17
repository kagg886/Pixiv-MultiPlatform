package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import korlibs.io.async.async
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import okio.Buffer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.epub.builder.EpubBuilder
import top.kagg886.epub.data.ResourceItem
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.BookmarkVisibility
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.novel.*
import top.kagg886.pixko.module.novel.parser.*
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.NovelHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.ui.util.NovelNodeElement
import top.kagg886.pmf.ui.util.container
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class NovelDetailViewModel(val id: Long) : ViewModel(), ScreenModel,
    ContainerHost<NovelDetailViewState, NovelDetailSideEffect>, KoinComponent {
    override val container: Container<NovelDetailViewState, NovelDetailSideEffect> =
        container(NovelDetailViewState.Loading) {
            reload()
        }
    private val client = PixivConfig.newAccountFromConfig()
    private val img by inject<HttpClient>()
    private val database by inject<AppDatabase>()

    fun reload() = intent {
        reduce {
            NovelDetailViewState.Loading
        }
        val result = kotlin.runCatching {
            client.getNovelDetail(id) to client.getNovelContent(id)
        }
        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
            reduce { NovelDetailViewState.Error() }
            return@intent
        }
        val (detail, content) = result.getOrThrow()
        val images = kotlin.runCatching { content.images }.getOrElse { emptyMap() }

        val nodeMap = linkedMapOf<Int, NovelNodeElement>()

        val data = kotlin.runCatching {
            content.data
        }

        if (data.isFailure) {
            reduce {
                NovelDetailViewState.Error("小说:${id}的正文解析失败惹")
            }
            return@intent
        }

        //异步获取image
        coroutineScope {
            for ((index, i) in data.getOrThrow().withIndex()) {
                when (i) {
                    is PlainTextNode -> {
                        nodeMap[index] = NovelNodeElement.Plain(i.text)
                    }

                    is JumpUriNode -> {
                        nodeMap[index] = NovelNodeElement.JumpUri(i.text, i.uri)
                    }

                    is NotationNode -> {
                        nodeMap[index] = NovelNodeElement.Notation(i.text, i.notation)
                    }

                    is UploadImageNode -> {
                        val priority = listOf(
                            NovelImagesSize.N480Mw,
                            NovelImagesSize.N1200x1200,
                            NovelImagesSize.N128x128,
                            NovelImagesSize.NOriginal,
                            NovelImagesSize.N240Mw
                        )
                        val img = priority.firstNotNullOf {
                            kotlin.runCatching {
                                images[i.url]!![it]
                            }.getOrNull()
                        }
                        nodeMap[index] = NovelNodeElement.UploadImage(img)
                    }

                    is PixivImageNode -> {
                        launch {
                            val illust = client.getIllustDetail(i.id.toLong())
                            nodeMap[index] = NovelNodeElement.PixivImage(
                                illust,
                                illust.contentImages[IllustImagesType.MEDIUM]?.get(i.index)!!
                            )
                        }
                    }

                    is NewPageNode -> {
                        nodeMap[index] = NovelNodeElement.NewPage(index + 1)
                    }

                    is TitleNode -> {
                        nodeMap[index] = NovelNodeElement.Title(i.text)
                    }

                    is JumpPageNode -> {
                        nodeMap[index] = NovelNodeElement.JumpPage(i.page)
                    }
                }
            }
        }
        reduce {
            NovelDetailViewState.Success(
                detail,
                content,
                nodeMap.toList().sortedBy { it.first }.map { it.second }
            )
        }
        if (AppConfig.recordNovelHistory) {
            database.novelHistoryDAO().insert(NovelHistory(id, detail, Clock.System.now().toEpochMilliseconds()))
        }
    }

    @OptIn(OrbitExperimental::class, ExperimentalUuidApi::class)
    fun exportToEpub() = intent {
        runOn<NovelDetailViewState.Success> {
            postSideEffect(NovelDetailSideEffect.Toast("正在导出，请稍等"))

            val coverImage = ResourceItem(
                file = Buffer().write(img.get(with(state.novel.imageUrls) { original ?: contentLarge }).bodyAsBytes()),
                extension = "png",
                mediaType = "image/png"
            )

            val inlineImages = state.nodeMap.map {
                async(Dispatchers.IO) {
                    when (it) {
                        is NovelNodeElement.PixivImage -> {
                            it to ResourceItem(
                                file = Buffer().write(img.post(it.url).bodyAsBytes()),
                                extension = "png",
                                mediaType = "image/png"
                            )
                        }

                        is NovelNodeElement.UploadImage -> {
                            it to ResourceItem(
                                file = Buffer().write(img.post(it.url).bodyAsBytes()),
                                extension = "png",
                                mediaType = "image/png"
                            )
                        }

                        else -> null
                    }
                }
            }.awaitAll().filterNotNull().toMap()

            val doc = Document.createShell("")
            var page = 1
            for (i in state.nodeMap) {
                when (i) {
                    is NovelNodeElement.Plain -> {
                        i.text.split("\n").map {
                            doc.body().appendElement("p").text(it)
                        }
                    }

                    is NovelNodeElement.JumpUri -> {
                        doc.body().appendElement("p")
                            .appendElement("a")
                            .attr("href", i.uri)
                            .text(i.text)
                    }

                    is NovelNodeElement.Notation -> {
                        doc.body().appendElement("span").text(i.text)
                    }

                    is NovelNodeElement.UploadImage -> {
                        doc.body().appendElement("img")
                            .attr("src", inlineImages[i]!!.fileName)
                            .attr("alt", Uuid.random().toHexString())
                    }

                    is NovelNodeElement.PixivImage -> {
                        doc.body().appendElement("img")
                            .attr("src", inlineImages[i]!!.fileName)
                            .attr("alt", Uuid.random().toHexString())
                    }

                    is NovelNodeElement.Title -> {
                        doc.body().appendElement("h1").text(i.text)
                    }

                    is NovelNodeElement.NewPage -> {
                        doc.body().appendElement("h1").text("#Chapter${page++}")
                    }

                    is NovelNodeElement.JumpPage -> {
                        doc.body().appendElement("a")
                            .attr("href", "#Chapter${i.page}")
                            .text("跳转到第${i.page}章节")
                    }
                }
            }

            val docResource = ResourceItem(
                file = Buffer().write(doc.html().encodeToByteArray()),
                extension = "html",
                mediaType = "application/xhtml+xml"
            )

            val epub = EpubBuilder(cachePath.resolve(Uuid.random().toHexString())) {
                metadata {
                    title(state.novel.title)
                    creator(state.novel.user.name)
                    description(state.novel.caption)
                    publisher("github @Pixiv-MultiPlatform")

                    meta { put("cover", coverImage.uuid) }
                }

                manifest {
                    add(coverImage)
                    add(docResource)
                    addAll(inlineImages.values)
                }

                spine {
                    toc(state.novel.title, docResource)
                }
            }

            val dst = cachePath.resolve("${state.novel.title}.epub")
            epub.writeTo(dst)
            shareFile(dst)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun likeNovel(
        visibility: BookmarkVisibility = BookmarkVisibility.PUBLIC,
        tags: List<Tag>? = null
    ) = intent {
        runOn<NovelDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.bookmarkNovel(id) {
                    this.tags = tags
                    this.visibility = visibility
                }
            }

            if (result.isFailure) {
                postSideEffect(NovelDetailSideEffect.Toast("收藏失败~"))
                return@runOn
            }
            reduce {
                state.copy(
                    novel = state.novel.copy(isBookmarked = true)
                )
            }
            postSideEffect(NovelDetailSideEffect.Toast("收藏成功~"))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeNovel() = intent {
        runOn<NovelDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.deleteBookmarkNovel(id)
            }

            if (result.isFailure) {
                postSideEffect(NovelDetailSideEffect.Toast("取消收藏失败~"))
                return@runOn
            }
            reduce {
                state.copy(
                    novel = state.novel.copy(isBookmarked = false)
                )
            }
            postSideEffect(NovelDetailSideEffect.Toast("取消收藏成功~"))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun followUser(private: Boolean = false) = intent {
        runOn<NovelDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.followUser(
                    state.novel.user.id,
                    publicity = if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC
                )
            }
            if (result.isFailure) {
                postSideEffect(NovelDetailSideEffect.Toast("关注失败~"))
                return@runOn
            }
            if (private) {
                postSideEffect(NovelDetailSideEffect.Toast("悄悄关注是不想让别人看到嘛⁄(⁄ ⁄•⁄ω⁄•⁄ ⁄)⁄"))
            } else {
                postSideEffect(NovelDetailSideEffect.Toast("关注成功~"))
            }
            reduce {
                state.copy(
                    novel = state.novel.copy(
                        user = state.novel.user.copy(
                            isFollowed = true
                        )
                    )
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
                postSideEffect(NovelDetailSideEffect.Toast("取关失败~(*^▽^*)"))
                return@runOn
            }
            postSideEffect(NovelDetailSideEffect.Toast("取关成功~o(╥﹏╥)o"))
            reduce {
                state.copy(
                    novel = state.novel.copy(
                        user = state.novel.user.copy(
                            isFollowed = false
                        )
                    )
                )
            }
        }
    }
}

sealed class NovelDetailViewState {
    data object Loading : NovelDetailViewState()
    data class Error(val cause: String = "加载失败惹~") : NovelDetailViewState()
    data class Success(val novel: Novel, val core: NovelData, val nodeMap: List<NovelNodeElement>) :
        NovelDetailViewState()
}

sealed class NovelDetailSideEffect {
    data class Toast(val msg: String) : NovelDetailSideEffect()
}
