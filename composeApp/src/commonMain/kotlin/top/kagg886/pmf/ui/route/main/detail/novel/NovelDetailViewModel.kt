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
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.loadImage
import top.kagg886.pixko.module.novel.*
import top.kagg886.pixko.module.novel.parser.NovelContentBlockType.*
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.NovelHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.container
import java.io.ByteArrayOutputStream
import java.util.*

class NovelDetailViewModel(val id: Long) : ViewModel(), ScreenModel,
    ContainerHost<NovelDetailViewState, NovelDetailSideEffect>, KoinComponent {
    override val container: Container<NovelDetailViewState, NovelDetailSideEffect> =
        container(NovelDetailViewState.Loading) {
            loadByNovelId(id)
        }
    private val client = PixivConfig.newAccountFromConfig()

    private val database by inject<AppDatabase>()

    fun loadByNovelId(id: Long) = intent {
        reduce {
            NovelDetailViewState.Loading
        }
        val result = kotlin.runCatching {
            client.getNovelDetail(id) to client.getNovelContent(id)
        }
        if (result.isFailure) {
            reduce { NovelDetailViewState.Error }
            return@intent
        }
        val (detail, content) = result.getOrThrow()
        val images = kotlin.runCatching { content.images }.getOrElse { emptyMap() }

        val nodeMap = linkedMapOf<Int, String?>()

        //异步获取image
        coroutineScope {
            for ((index, i) in content.data.withIndex()) {
                when (i.novelContentBlockType) {
                    PLAIN -> {
                        nodeMap[index] = i.value
                    }

                    JUMP_URI -> {
                        nodeMap[index] = "[${i.value}](${i.metadata})"
                    }

                    NOTATION -> {
                        nodeMap[index] = i.value
                    }

                    UPLOAD_IMAGE -> {
                        nodeMap[index] = "\n![上传图片](${images[i.value]!![NovelImagesSize.N480Mw]})\n"
                    }

                    PIXIV_IMAGE -> {
                        launch {
                            val url =
                                client.getIllustDetail(i.value!!.toLong()).contentImages[IllustImagesType.MEDIUM]?.get(0)!!
                            nodeMap[index] = "\n![${i.value}](${url})\n"
                        }
                    }

                    NEW_PAGE -> {
//                    nodeMap[index] = "\n***\n# Page ${pageIndex + 1}\n"
//                    pageIndex++
                    }

                    TITLE -> {
                        nodeMap[index] = "\n#### ${i.value}\n"
                    }

                    JUMP_PAGE -> {
//                    nodeMap[index] = "[跳转到第${i.value}页](#Page ${i.value})"
                    }
                }
            }
        }
        reduce {
            NovelDetailViewState.Success(
                detail,
                content,
                nodeMap.toSortedMap().values.joinToString("") { it ?: "" }
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

                val uploadImages = kotlin.runCatching { state.core.images }.getOrElse { emptyMap() }


                for (i in state.core.data) {
                    when (i.novelContentBlockType) {
                        PLAIN -> {
                            i.value!!.split("\n").map {
                                doc.body().appendElement("p").text(it)
                            }
                        }

                        JUMP_URI -> {
                            doc.body().appendElement("a").attr("href", i.metadata!!).text(i.value!!)
                        }

                        NOTATION -> {
                            doc.body().appendElement("span").text(i.value!!)
                        }

                        UPLOAD_IMAGE -> {
                            val a = uploadImages[i.value!!]!![NovelImagesSize.N480Mw] as String
                            val uuid = UUID.randomUUID().toString().replace("-", "") + ".png"
                            addResource(Resource(client.loadImage(a), uuid))
                            doc.body().appendElement("img").attr("src", uuid).attr("alt", i.value!!)
                        }

                        PIXIV_IMAGE -> {
                            val img =
                                client.getIllustDetail(i.value!!.toLong()).contentImages[IllustImagesType.LARGE]!![0]
                            val uuid = UUID.randomUUID().toString().replace("-", "") + ".png"
                            addResource(Resource(client.loadImage(img), uuid))
                            doc.body().appendElement("img").attr("src", uuid).attr("alt", i.value!!)
                        }

                        TITLE -> {
                            doc.body().appendElement("h1").text(i.value!!)
                        }

                        NEW_PAGE -> {
                            addSection(
                                "第${pageIndex + 1}页",
                                Resource(doc.html().toByteArray(), "page_${pageIndex}.html")
                            )
                            doc = Document.createShell("")
                            pageIndex++
                        }

                        JUMP_PAGE -> {
                            doc.body().appendElement("a").attr("href", "page_${i.value!!.toInt() - 1}.html")
                                .text("跳转到第${i.value}页")
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
}

sealed class NovelDetailViewState {
    data object Loading : NovelDetailViewState()
    data object Error : NovelDetailViewState()
    data class Success(val novel: Novel, val core: NovelData, val content: String) : NovelDetailViewState()
}

sealed class NovelDetailSideEffect {
    data class Toast(val msg: String) : NovelDetailSideEffect()
}