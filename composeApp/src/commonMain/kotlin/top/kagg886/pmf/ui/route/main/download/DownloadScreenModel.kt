package top.kagg886.pmf.ui.route.main.download

import androidx.lifecycle.ViewModel
import arrow.fx.coroutines.fixedRate
import arrow.fx.coroutines.raceN
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.counted
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import okio.Buffer as OkioBuffer
import okio.Path
import okio.Sink as OkioSink
import okio.buffer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.dataPath
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.DownloadItem
import top.kagg886.pmf.download_completed
import top.kagg886.pmf.download_failed
import top.kagg886.pmf.download_started
import top.kagg886.pmf.task_already_exists
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.deleteRecursively
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.listFile
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.util.mkdirs
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source
import top.kagg886.pmf.util.zip

fun DownloadItem.downloadRootPath(): Path = dataPath.resolve("download").resolve(id.toString())

class DownloadScreenModel :
    ContainerHost<DownloadScreenState, DownloadScreenSideEffect>,
    ViewModel(),
    ScreenModel,
    KoinComponent {
    private val database by inject<AppDatabase>()

    private val net by inject<HttpClient>()

    private val jobs = mutableMapOf<Long, Job>()

    @OptIn(OrbitExperimental::class)
    fun startDownload(illust: Illust): Job? {
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
                if (file.exists()) {
                    logger.d("the illust:${illust.id} has been downloaded, delete it")
                    // 有的话就递归删除重下
                    file.deleteRecursively()
                }
                file.mkdirs()

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
                                    (file / "$index.png").sink().asRawSink().use { sink ->
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
                    file.deleteRecursively()
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

    fun saveToExternalFile(it: DownloadItem) = intent {
        val listFiles = it.downloadRootPath().listFile()
        if (listFiles.size == 1) {
            val platformFile = FileKit.openFileSaver(
                suggestedName = it.illust.title,
                extension = "png",
            )
            platformFile?.write(listFiles[0].source().buffer().readByteArray())
            return@intent
        }
        val platformFile = FileKit.openFileSaver(
            suggestedName = "${it.illust.title}(${it.id})",
            extension = "zip",
        )
        platformFile?.write(
            it.downloadRootPath().zip(
                target = cachePath.resolve("share")
                    .resolve("${it.id}.zip"),
            ).source().buffer().readByteArray()
        )
    }

    fun shareFile(it: DownloadItem) = intent {
        val listFiles = it.downloadRootPath().listFile()
        if (listFiles.size == 1) {
            top.kagg886.pmf.shareFile(listFiles[0])
            return@intent
        }
        top.kagg886.pmf.shareFile(
            it.downloadRootPath().zip(
                target = cachePath.resolve("${it.id}.zip"),
            ),
        )
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
    data class Loaded(val illust: Flow<List<DownloadItem>>) : DownloadScreenState()
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
