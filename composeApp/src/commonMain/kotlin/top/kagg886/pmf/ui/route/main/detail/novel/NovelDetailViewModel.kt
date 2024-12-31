package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaInstant
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Date
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import org.jsoup.nodes.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.BookmarkVisibility
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.loadImage
import top.kagg886.pixko.module.novel.*
import top.kagg886.pixko.module.novel.parser.*
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.NovelHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.NovelNodeElement
import top.kagg886.pmf.ui.util.container
import java.io.ByteArrayOutputStream
import java.util.*

class NovelDetailViewModel(val id: Long) : ViewModel(), ScreenModel,
    ContainerHost<NovelDetailViewState, NovelDetailSideEffect>, KoinComponent {
    override val container: Container<NovelDetailViewState, NovelDetailSideEffect> =
        container(NovelDetailViewState.Loading) {
            reload()
        }
    private val client = PixivConfig.newAccountFromConfig()

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
                nodeMap.toSortedMap { a, b -> a - b }
            )
        }
        if (AppConfig.recordNovelHistory) {
            database.novelHistoryDAO().insert(NovelHistory(id, detail, System.currentTimeMillis()))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun exportToEpub() = intent {
        runOn<NovelDetailViewState.Success> {
            postSideEffect(NovelDetailSideEffect.Toast("正在导出，请稍等"))
            val book = Book().apply {
                with(metadata) {
                    addTitle(state.novel.title)
                    addAuthor(Author(state.novel.user.name))
                    addDescription(state.novel.caption)
                    addDate(Date(java.util.Date.from(state.novel.createDate.toJavaInstant())))
                    addPublisher("github @Pixiv-MultiPlatform")

                    coverImage = Resource(
                        client.loadImage(state.novel.imageUrls.content),
                        "cover.png"
                    )
                }
                var doc = Document.createShell("")
                var pageIndex = 0


                for ((index, i) in state.nodeMap) {
                    when (i) {
                        is NovelNodeElement.Plain -> {
                            i.text.split("\n").map {
                                doc.body().appendElement("p").text(it)
                            }
                        }

                        is NovelNodeElement.JumpUri -> {
                            doc.body().appendElement("a").attr("href", i.uri).text(i.text)
                        }

                        is NovelNodeElement.Notation -> {
                            doc.body().appendElement("span").text(i.text)
                        }

                        is NovelNodeElement.UploadImage -> {
                            val uuid = UUID.randomUUID().toString().replace("-", "") + ".png"
                            addResource(Resource(client.loadImage(i.url), uuid))
                            doc.body().appendElement("img").attr("src", uuid).attr("alt", "upload-image")
                        }

                        is NovelNodeElement.PixivImage -> {
                            val img = i.illust
                            val uuid = UUID.randomUUID().toString().replace("-", "") + ".png"
                            addResource(
                                Resource(
                                    client.loadImage(img.contentImages[IllustImagesType.LARGE]!![0]),
                                    uuid
                                )
                            )
                            doc.body().appendElement("img").attr("src", uuid).attr("alt", img.title)
                        }

                        is NovelNodeElement.Title -> {
                            doc.body().appendElement("h1").text(i.text)
                        }

                        is NovelNodeElement.NewPage -> {
                            addSection(
                                "第${pageIndex + 1}页",
                                Resource(doc.html().toByteArray(), "page_${pageIndex}.html")
                            )
                            doc = Document.createShell("")
                            pageIndex++
                        }

                        is NovelNodeElement.JumpPage -> {
                            doc.body().appendElement("a").attr("href", "page_${i.page - 1}.html")
                                .text("跳转到第${i.page}页")
                        }
                    }
                }
                addSection(
                    "第${pageIndex + 1}页",
                    Resource(doc.html().toByteArray(), "page_${pageIndex}.html")
                )
            }

            val bytes = with(ByteArrayOutputStream()) {
                EpubWriter().write(book, this)

                toByteArray()
            }
            postSideEffect(NovelDetailSideEffect.Toast("导出完毕。"))
            FileKit.saveFile(
                bytes = bytes,
                extension = "epub",
                baseName = state.novel.title
            )

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
                client.followUser(state.novel.user.id, publicity = if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC)
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
    data class Success(val novel: Novel, val core: NovelData, val nodeMap: Map<Int, NovelNodeElement>) :
        NovelDetailViewState()
}

sealed class NovelDetailSideEffect {
    data class Toast(val msg: String) : NovelDetailSideEffect()
}