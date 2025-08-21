package top.kagg886.pmf.ui.route.main.download

import androidx.lifecycle.ViewModel
import arrow.fx.coroutines.fixedRate
import arrow.fx.coroutines.raceN
import cafe.adriel.voyager.core.model.ScreenModel
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Entities
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import korlibs.io.async.async
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import okio.Buffer as OkioBuffer
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink as OkioSink
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
import top.kagg886.pixko.PixivAccount
import top.kagg886.pixko.anno.ExperimentalNovelParserAPI
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.novel.NovelImagesSize
import top.kagg886.pixko.module.novel.getNovelContent
import top.kagg886.pixko.module.novel.parser.v2.JumpPageNode
import top.kagg886.pixko.module.novel.parser.v2.JumpUriNode
import top.kagg886.pixko.module.novel.parser.v2.NewPageNode
import top.kagg886.pixko.module.novel.parser.v2.PixivImageNode
import top.kagg886.pixko.module.novel.parser.v2.TextNode
import top.kagg886.pixko.module.novel.parser.v2.TitleNode
import top.kagg886.pixko.module.novel.parser.v2.UploadImageNode
import top.kagg886.pixko.module.novel.parser.v2.content
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.*
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.DownloadItem
import top.kagg886.pmf.backend.database.dao.DownloadItemType
import top.kagg886.pmf.backend.database.dao.illust
import top.kagg886.pmf.backend.database.dao.novel
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.download_completed
import top.kagg886.pmf.download_failed
import top.kagg886.pmf.download_root_not_set
import top.kagg886.pmf.download_root_permission_revoked
import top.kagg886.pmf.download_started
import top.kagg886.pmf.jump_to_chapter
import top.kagg886.pmf.task_already_exists
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.*

class DownloadScreenModel :
    ContainerHost<DownloadScreenState, DownloadScreenSideEffect>,
    ViewModel(),
    ScreenModel,
    KoinComponent {
    private val database by inject<AppDatabase>()

    private val net by inject<HttpClient>()
    private val pixiv = PixivConfig.newAccountFromConfig()

    private val jobs = mutableMapOf<Long, Job>()

    private var internalSystem = lazy {
        // make it lazy to upgrade.
        safFileSystem(AppConfig.downloadUri)
    }

    private val system by internalSystem

    private fun DownloadItem.downloadRootPath(): Path = when (meta) {
        DownloadItemType.ILLUST -> id.toString()
        DownloadItemType.NOVEL -> "${novel.title} - ${novel.user.name}.epub"
    }.toPath()

    fun startIllustDownloadOr(item: DownloadItem, orElse: () -> Unit = {}) = intent {
        if (!system.exists(item.downloadRootPath())) {
            startIllustDownload(item.illust)?.join()
            return@intent
        }
        orElse()
    }

    fun startNovelDownloadOr(item: DownloadItem, orElse: () -> Unit = {}) = intent {
        if (!system.exists(item.downloadRootPath())) {
            startNovelDownload(item.novel)?.join()
            return@intent
        }
        orElse()
    }

    fun setSAFSystem(uri: String) {
        internalSystem = lazy {
            safFileSystem(uri)
        }
    }

    fun stopAll(): Job = intent {
        for (job in jobs.values) {
            job.cancel()
        }
        jobs.clear()
    }

    @OptIn(OrbitExperimental::class)
    fun startIllustDownload(illust: Illust): Job? {
        if (illust.id.toLong() in jobs.keys) {
            intent {
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.task_already_exists),
                        true,
                    ),
                )
            }
            return null
        }
        val job = intent {
            if (AppConfig.downloadUri.isEmpty() && currentPlatform !is Platform.Apple) { // 检查uri是否成功设置
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_root_not_set),
                        false,
                    ),
                )
                jobs.remove(illust.id.toLong())
                return@intent
            }

            if (!system.exists("/".toPath())) {
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_root_permission_revoked),
                        false,
                    ),
                )
                jobs.remove(illust.id.toLong())
                return@intent
            }

            runOn<DownloadScreenState.Loaded> {
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_started),
                        true,
                    ),
                )
                val dao = database.downloadDAO()

                // 查找历史记录任务，若无任务的话则新建任务并插入
                val task = dao.find(illust.id.toLong())?.copy(success = false) ?: DownloadItem(
                    id = illust.id.toLong(),
                    illust = illust,
                    success = false,
                ).apply {
                    logger.d("create a record for illust:${illust.id} where not exists in database")
                    dao.insert(this)
                }

                // 获取下载的根目录
                val file = task.downloadRootPath()
                logger.d("the illust:${illust.id} will be download to $file")
                if (system.exists(file)) {
                    logger.d("the illust:${illust.id} has been downloaded, delete it")
                    // 有的话就递归删除重下
                    system.deleteRecursively(file)
                }
                system.createDirectories(file)

                // 获取所有下载链接
                val urls = illust.contentImages[IllustImagesType.ORIGIN]!!

                logger.d("the illust:${illust.id}'s download link: $urls")

                // 更新DAO层
                dao.update(task.copy(progress = 0f))
                val result = runCatching {
                    coroutineScope {
                        val size = atomic(0L)
                        val lengths = urls.map { atomic(0L) }
                        (urls zip lengths).mapIndexed { index, (url, atom) ->
                            async(Dispatchers.IO) {
                                net.prepareGet(url) {
                                    timeout {
                                        socketTimeoutMillis = 1.hours.inWholeMilliseconds
                                        connectTimeoutMillis = 1.hours.inWholeMilliseconds
                                        requestTimeoutMillis = 1.hours.inWholeMilliseconds
                                    }
                                }.execute { resp ->
                                    logger.d("the illust:${illust.id}'s download link: $url, status: ${resp.status}")
                                    val length = resp.contentLength()!!
                                    size.update { v -> v + length }
                                    logger.d("the illust:${illust.id}'s download link: $url, length is: $length")

                                    val src = resp.bodyAsChannel().counted()
                                    system.sink(file / "$index.png").asRawSink().use { sink ->
                                        raceN(
                                            {
                                                val upd = {
                                                    val bytes = src.totalBytesRead
                                                    atom.value = bytes
                                                    logger.v("Illust:${illust.id}'s download link: $url, fetched data size: $bytes")
                                                }
                                                try {
                                                    fixedRate(200.milliseconds).collect { upd() }
                                                } finally {
                                                    upd()
                                                }
                                            },
                                            { src.copyAndClose(sink.asByteWriteChannel()) },
                                        )
                                    }
                                }
                            }
                        }.apply {
                            raceN(
                                { awaitAll() },
                                {
                                    fixedRate(200.milliseconds).collect {
                                        val download = lengths.sumOf { v -> v.value }
                                        if (download == 0L || size.value == 0L) return@collect
                                        dao.update(task.copy(progress = download.toFloat() / size.value.toFloat()))
                                    }
                                },
                            )
                        }
                    }
                }

                // 移除注册的任务
                jobs.remove(illust.id.toLong())
                if (result.isFailure) {
                    // 失败则报错
                    dao.update(task.copy(progress = -1f))
                    logger.e(result.exceptionOrNull()!!) { "Illust: [${illust.title}(${illust.id})] download failed: ${result.exceptionOrNull()?.message}" }
                    postSideEffect(
                        DownloadScreenSideEffect.Toast(
                            getString(Res.string.download_failed, illust.title, illust.id),
                        ),
                    )
                    system.deleteRecursively(file)
                    return@runOn
                }
                dao.update(task.copy(success = true, progress = -1f))
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_completed, illust.title, illust.id),
                    ),
                )
            }
        }
        jobs[illust.id.toLong()] = job
        return job
    }

    @OptIn(OrbitExperimental::class, ExperimentalNovelParserAPI::class)
    fun startNovelDownload(novel: top.kagg886.pixko.module.novel.Novel): Job? {
        if (novel.id.toLong() in jobs.keys) {
            intent {
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.task_already_exists),
                        true,
                    ),
                )
            }
            return null
        }
        val job = intent {
            if (AppConfig.downloadUri.isEmpty() && currentPlatform !is Platform.Apple) { // 检查uri是否成功设置
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_root_not_set),
                        false,
                    ),
                )
                jobs.remove(novel.id.toLong())
                return@intent
            }

            if (!system.exists("/".toPath())) {
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_root_permission_revoked),
                        false,
                    ),
                )
                jobs.remove(novel.id.toLong())
                return@intent
            }

            runOn<DownloadScreenState.Loaded> {
                val dao = database.downloadDAO()
                // 查找历史记录任务，若无任务的话则新建任务并插入
                val task = dao.find(novel.id.toLong())?.copy(success = false) ?: DownloadItem(
                    id = novel.id.toLong(),
                    novel = novel,
                    success = false,
                ).apply {
                    logger.d("create a record for novel:${novel.id} where not exists in database")
                    dao.insert(this)
                }

                val file = task.downloadRootPath()
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_started),
                        true,
                    ),
                )

                dao.update(task.copy(progress = 0f))
                val result = runCatching {
                    if (system.exists(file)) {
                        system.delete(file)
                    }

                    val coverImage = ResourceItem(
                        file = OkioBuffer().write(
                            net.get(
                                with(novel.imageUrls) {
                                    original ?: contentLarge
                                },
                            ).bodyAsBytes(),
                        ),
                        extension = "png",
                        mediaType = "image/png",
                        properties = "cover-image",
                    )

                    val content = pixiv.getNovelContent(novel.id.toLong())
                    val result = content.content.value

                    val inlineImages = result.filter { it is PixivImageNode || it is UploadImageNode }.map { node ->
                        async(Dispatchers.IO) {
                            val buf = when (node) {
                                is PixivImageNode -> {
                                    OkioBuffer().write(
                                        net.get(pixiv.getIllustDetail(node.id.toLong()).imageUrls.content)
                                            .bodyAsBytes(),
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
                                            content.images[node.url]!![it]
                                        }.getOrNull()
                                    }
                                    OkioBuffer().write(net.get(img).bodyAsBytes())
                                }

                                else -> error("Unexpected node type ${node::class}")
                            }

                            node to ResourceItem(
                                file = buf,
                                extension = "png",
                                mediaType = "image/png",
                            )
                        }
                    }.awaitAll().toMap()

                    val doc = Document.createShell("").apply {
                        outputSettings()
                            .syntax(Document.OutputSettings.Syntax.xml)
                            .escapeMode(Entities.EscapeMode.xhtml)
                            .charset("UTF-8")
                            .prettyPrint(false)

                        selectFirst("html")?.attr("xmlns", "http://www.w3.org/1999/xhtml")
                    }
                    var page = 1
                    for (i in result) {
                        when (i) {
                            is TitleNode -> {
                                doc.body().appendElement("h1").text(i.text.toString())
                            }

                            is TextNode -> {
                                doc.body().appendElement("p").text(i.text.toString().replace("\\n", "\n"))
                            }

                            is JumpPageNode -> {
                                doc.body().appendElement("a").attr("href", "#Chapter${i.page}")
                                    .text(getString(Res.string.jump_to_chapter, i.page))
                            }

                            is JumpUriNode -> {
                                doc.body().appendElement("p").appendElement("a").attr("href", i.uri)
                                    .text(i.text)
                            }

                            is NewPageNode -> {
                                val page = ++page
                                doc.body().appendElement("h1").text("Chapter$page")
                                    .id("Chapter$page")
                                    .attr("style", "height: 0;overflow: hidden;visibility: hidden;page-break-before: always;")
                            }

                            is PixivImageNode -> doc.body().appendElement("img")
                                .attr("src", inlineImages[i]!!.fileName)
                                .attr("alt", Uuid.random().toHexString())

                            is UploadImageNode -> {
                                doc.body().appendElement("img").attr("src", inlineImages[i]!!.fileName)
                                    .attr("alt", Uuid.random().toHexString())
                            }
                        }
                    }

                    val docResource = ResourceItem(
                        file = OkioBuffer().write(doc.html().encodeToByteArray()),
                        extension = "html",
                        mediaType = "application/xhtml+xml",
                    )

                    val epub = EpubBuilder(cachePath.resolve(Uuid.random().toHexString())) {
                        metadata {
                            title(novel.title)
                            creator(novel.user.name)
                            description(novel.caption)
                            publisher("github @Pixiv-MultiPlatform")
                            language("zh-CN")

                            meta {
                                put("cover", coverImage.uuid)
                            }
                        }

                        manifest {
                            add(coverImage)
                            add(docResource)
                            addAll(inlineImages.values)
                        }

                        spine {
                            toc(novel.title, docResource)
                        }
                    }

                    useTempFile { f ->
                        epub.writeTo(f)

                        f.source().use { i ->
                            system.sink(file).use { o ->
                                i.transfer(o)
                            }
                        }
                    }
                }

                // 移除注册的任务
                jobs.remove(novel.id.toLong())

                if (result.isFailure) {
                    dao.update(task.copy(progress = -1f))
                    logger.e(result.exceptionOrNull()!!) { "Novel: [${novel.title}(${novel.id})] download failed: ${result.exceptionOrNull()?.message}" }
                    postSideEffect(
                        DownloadScreenSideEffect.Toast(
                            getString(Res.string.download_failed, novel.title, novel.id),
                        ),
                    )
                    system.delete(file)
                    return@runOn
                }

                dao.update(task.copy(success = true, progress = -1f))
                postSideEffect(
                    DownloadScreenSideEffect.Toast(
                        getString(Res.string.download_completed, novel.title, novel.id),
                    ),
                )
            }
        }
        jobs[novel.id.toLong()] = job
        return job
    }

    fun saveToExternalFile(it: DownloadItem) = intent {
        if (!system.metadata(it.downloadRootPath()).isDirectory) {
            val ext = when (it.meta) {
                DownloadItemType.ILLUST -> "png"
                DownloadItemType.NOVEL -> "epub"
            }
            val platformFile = FilePicker.openFileSaver(
                suggestedName = it.title,
                extension = ext,
            )
            system.source(it.downloadRootPath()).use { source ->
                platformFile?.use { sink ->
                    source.transfer(sink)
                }
            }
            return@intent
        }

        val listFiles = system.list(it.downloadRootPath())
        if (listFiles.size == 1) {
            val platformFile = FilePicker.openFileSaver(
                suggestedName = it.title,
                extension = "png",
            )
            platformFile?.buffer()?.use { buf -> buf.write(system.source(listFiles[0]).buffer().readByteArray()) }
            return@intent
        }
        val platformFile = FilePicker.openFileSaver(
            suggestedName = "${it.title}(${it.id})",
            extension = "zip",
        )

        if (platformFile == null) {
            return@intent
        }

        useTempDir { dir ->
            for (image in listFiles) {
                dir.resolve(image.name).sink().use { sink ->
                    system.source(image).use { source ->
                        source.transfer(sink)
                    }
                }
            }

            dir.zip().apply {
                source().use { source -> source.transfer(platformFile) }
                delete()
            }
        }
    }

    fun shareFile(it: DownloadItem) = intent {
        if (!system.metadata(it.downloadRootPath()).isDirectory) {
            val ext = when (it.meta) {
                DownloadItemType.ILLUST -> "png"
                DownloadItemType.NOVEL -> "epub"
            }
            useTempFile { target ->
                system.source(it.downloadRootPath()).use { source ->
                    target.resolve("${it.title}.$ext").sink().use { sink ->
                        source.transfer(sink)
                    }
                }
                top.kagg886.pmf.shareFile(target)
            }
            return@intent
        }
        val listFiles = system.list(it.downloadRootPath())

        // transfer it to app cache.
        useTempDir { dir ->
            for (image in listFiles) {
                dir.resolve(image.name).sink().use { sink ->
                    system.source(image).use { source ->
                        source.transfer(sink)
                    }
                }
            }

            if (listFiles.size == 1) {
                top.kagg886.pmf.shareFile(dir.listFile()[0])
                return@intent
            }

            top.kagg886.pmf.shareFile(dir.zip(cachePath.resolve("${Uuid.random().toHexString()}.zip")))
        }
    }

    override val container: Container<DownloadScreenState, DownloadScreenSideEffect> =
        container(DownloadScreenState.Loading) {
            val data = database.downloadDAO().allSuspend()
            for (i in data) {
                if (!i.success) {
                    database.downloadDAO().update(i.copy(success = false, progress = -1f))
                }
            }
            reduce {
                DownloadScreenState.Loaded(database.downloadDAO().all())
            }
        }
}

sealed class DownloadScreenState {
    data object Loading : DownloadScreenState()
    data class Loaded(val data: Flow<List<DownloadItem>>) : DownloadScreenState()
}

sealed class DownloadScreenSideEffect {
    data class Toast(val msg: String, val jump: Boolean = false) : DownloadScreenSideEffect()
}

@OptIn(UnsafeIoApi::class)
fun OkioSink.asRawSink(): RawSink = let { sink ->
    object : RawSink {
        private val buffer = OkioBuffer()

        override fun write(source: Buffer, byteCount: Long) {
            require(source.size >= byteCount) {
                "Buffer does not contain enough bytes to write. Requested $byteCount, actual size is ${source.size}"
            }
            var remaining = byteCount
            while (remaining > 0) {
                UnsafeBufferOperations.readFromHead(source) { data, from, to ->
                    val toRead = min((to - from).toLong(), remaining).toInt()
                    remaining -= toRead
                    buffer.write(data, from, toRead)
                    toRead
                }
            }
            sink.write(buffer, byteCount)
        }

        override fun flush() = sink.flush()
        override fun close() = sink.close()
    }
}
