package top.kagg886.pmf.ui.route.main.download

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.core.FileKit
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.readByteArray
import okio.Buffer
import okio.Path
import okio.buffer
import okio.use
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
                    dao.insert(this)
                }

                //获取下载的根目录
                val file = task.downloadRootPath()
                if (file.exists()) {
                    //有的话就递归删除重下
                    file.deleteRecursively()
                }
                file.mkdirs()

                //获取所有下载链接
                val urls = illust.contentImages[IllustImagesType.ORIGIN]!!

                //更新DAO层
                dao.update(task.copy(progress = 0f))
                val result = kotlin.runCatching {
                    //计算全部大小。
                    //受限于ktor功能，只能分两次请求。
                    val allSize = coroutineScope {
                        val result = urls.map {
                            async(Dispatchers.IO) {
                                val resp = net.prepareGet(it).execute {
                                    it
                                }
                                resp.headers["Content-Length"]!!.toLong()
                            }
                        }.awaitAll()

                        result.sum()
                    }

                    val x = Mutex()
                    var size = 0f
                    coroutineScope {
                        urls.mapIndexed { index, url ->
                            //子下载
                            val download = file.resolve("$index.png").sink().buffer()
                            async(Dispatchers.IO) {
                                download.use {
                                    net.prepareGet(url).execute {
                                        val channel = it.bodyAsChannel()
                                        while (!channel.isClosedForRead) {
                                            val buf = channel.readRemaining(1024).readByteArray()

                                            download.write(
                                                buf
                                            )

                                            //协程安全地更新变量
                                            x.withLock {
                                                size += buf.size.toFloat()
                                            }
                                            dao.update(task.copy(progress = size / allSize))
                                        }
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                }

                //移除注册的任务
                jobs.remove(illust.id.toLong())
                if (result.isFailure) {
                    //失败则报错
                    dao.update(task.copy(progress = -1f))
                    Logger.e(result.exceptionOrNull()!!) { "Illust: [${illust.title}(${illust.id})] download failed: ${result.exceptionOrNull()?.message}" }
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
                if (!i.downloadRootPath().exists()) {
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
