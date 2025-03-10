package top.kagg886.pmf.ui.route.main.download

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.core.FileKit
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.dataPath
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.DownloadItem
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.*
import kotlin.time.Duration.Companion.hours

fun DownloadItem.downloadRootPath(): Path {
    return dataPath.resolve("download").resolve(id.toString())
}

class DownloadScreenModel : ContainerHost<DownloadScreenState, DownloadScreenSideEffect>, ViewModel(), ScreenModel,
    KoinComponent {
    private val database by inject<AppDatabase>()

    private val net by inject<HttpClient>()

    private val jobs = mutableMapOf<Long, Job>()

    @OptIn(OrbitExperimental::class)
    fun startDownload(illust: Illust): Job? {
        if (illust.id.toLong() in jobs.keys) {
            intent {
                postSideEffect(DownloadScreenSideEffect.Toast("任务已存在且正在下载中，请前往下载页面查看", true))
            }
            return null
        }
        val job = intent {
            runOn<DownloadScreenState.Loaded> {
                postSideEffect(DownloadScreenSideEffect.Toast("下载已开始，是否跳转到下载页？", true))
                val dao = database.downloadDAO()

                //查找历史记录任务，若无任务的话则新建任务并插入
                val task = dao.find(illust.id.toLong())?.copy(success = false) ?: DownloadItem(
                    id = illust.id.toLong(),
                    illust = illust,
                    success = false
                ).apply {
                    logger.d("create a record for illust:${illust.id} where not exists in database")
                    dao.insert(this)
                }

                //获取下载的根目录
                val file = task.downloadRootPath()
                logger.d("the illust:${illust.id} will be download to $file")
                if (file.exists()) {
                    logger.d("the illust:${illust.id} has been downloaded, delete it")
                    //有的话就递归删除重下
                    file.deleteRecursively()
                }
                file.mkdirs()

                //获取所有下载链接
                val urls = illust.contentImages[IllustImagesType.ORIGIN]!!

                logger.d("the illust:${illust.id}'s download link: $urls")

                //更新DAO层
                dao.update(task.copy(progress = 0f))
                val result = kotlin.runCatching {
                    coroutineScope {
                        val lock = Mutex()
                        var size = 0L
                        var download = 0L

                        urls.mapIndexed { index, it ->
                            launch(Dispatchers.IO) {
                                net.prepareGet(it) {
                                    timeout {
                                        socketTimeoutMillis = 1.hours.inWholeMilliseconds
                                        connectTimeoutMillis = 1.hours.inWholeMilliseconds
                                        requestTimeoutMillis = 1.hours.inWholeMilliseconds
                                    }
                                }.execute { resp ->
                                    logger.d("the illust:${illust.id}'s download link: $it, status: ${resp.status}")
                                    val length = resp.headers[HttpHeaders.ContentLength]!!.toLong()
                                    lock.withLock { size += length }
                                    logger.d("the illust:${illust.id}'s download link: $it, length is: $length")

                                    val source = resp.bodyAsChannel().asOkioSource() //closed when around execute block
                                    val sink = file.resolve("$index.png").sink().buffer()

                                    sink.use {
                                        var len: Long
                                        val buffer = Buffer()
                                        while (source.read(buffer, 8192).also { len = it } != -1L) {
                                            sink.write(buffer.readByteArray())
                                            sink.flush()
                                            buffer.clear()
                                            lock.withLock {
                                                logger.v("the illust:${illust.id}'s download link: $it, fetch data size: $len")
                                                download += len
                                                dao.update(
                                                    task.copy(
                                                        progress = download.toFloat() / size.toFloat()
                                                    )
                                                )
//                                                logger.d("the illust:${illust.id}'s progress: ${download.toFloat() / size.toFloat()}")
                                            }
                                        }
                                    }
                                }
                            }
                        }.joinAll()
                    }
                }

                //移除注册的任务
                jobs.remove(illust.id.toLong())
                if (result.isFailure) {
                    //失败则报错
                    dao.update(task.copy(progress = -1f))
                    logger.e(result.exceptionOrNull()!!) { "Illust: [${illust.title}(${illust.id})] download failed: ${result.exceptionOrNull()?.message}" }
                    postSideEffect(DownloadScreenSideEffect.Toast("${illust.title}(${illust.id})下载失败"))
                    file.deleteRecursively()
                    return@runOn
                }
                dao.update(task.copy(success = true, progress = -1f))
                postSideEffect(DownloadScreenSideEffect.Toast("${illust.title}(${illust.id})下载完成！"))
            }
        }
        jobs[illust.id.toLong()] = job
        return job
    }

    fun saveToExternalFile(it: DownloadItem) = intent {
        val listFiles = it.downloadRootPath().listFile()
        if (listFiles.size == 1) {
            FileKit.saveFile(
                bytes = listFiles[0].source().buffer().readByteArray(),
                baseName = it.illust.title,
                extension = "png"
            )
            return@intent
        }
        FileKit.saveFile(
            bytes = it.downloadRootPath().zip(
                target = cachePath.resolve("share")
                    .resolve("${it.id}.zip")
            ).source().buffer().readByteArray(),
            baseName = "${it.illust.title}(${it.id})",
            extension = "zip"
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
                target = cachePath.resolve("${it.id}.zip")
            )
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


private fun ByteReadChannel.asOkioSource(): Source {
    val channel = this
    return object : Source {
        override fun close() {
            channel.cancel()
        }

        override fun read(sink: Buffer, byteCount: Long): Long = runBlocking {
            val buf = ByteArray(byteCount.toInt())
            val len = channel.readAvailable(buf)
            if (len == -1) {
                return@runBlocking -1L
            }

            sink.write(buf, 0, len)

            len.toLong()
        }

        override fun timeout(): Timeout = Timeout.NONE
    }
}
