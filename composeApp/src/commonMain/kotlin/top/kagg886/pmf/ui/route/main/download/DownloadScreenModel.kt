package top.kagg886.pmf.ui.route.main.download

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import top.kagg886.pmf.backend.dataPath
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.DownloadItem
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.deleteRecursively
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.mkdirs
import top.kagg886.pmf.util.sink

fun DownloadItem.downloadRootPath(): Path {
    return dataPath.resolve("download").resolve(id.toString())
}

class DownloadScreenModel : ContainerHost<DownloadScreenState, DownloadScreenSideEffect>, ViewModel(), ScreenModel,
    KoinComponent {
    private val database by inject<AppDatabase>()

    private val net by inject<HttpClient>()

    private val jobs = mutableMapOf<Long, Job>()

    @OptIn(OrbitExperimental::class)
    fun startDownload(illust: Illust): Boolean {
        if (illust.id.toLong() in jobs.keys) {
            intent {
                postSideEffect(DownloadScreenSideEffect.Toast("任务已存在且正在下载中，请前往下载页面查看", true))
            }
            return false
        }
        jobs[illust.id.toLong()] = intent {
            runOn<DownloadScreenState.Loaded> {
                postSideEffect(DownloadScreenSideEffect.Toast("下载已开始，是否跳转到下载页？", true))
                val dao = database.downloadDAO()
                //查找任务，若无则新建
                val task = dao.find(illust.id.toLong())?.copy(success = false) ?: DownloadItem(
                    id = illust.id.toLong(),
                    illust = illust,
                    success = false
                ).apply {
                    dao.insert(this)
                }
                val file = task.downloadRootPath()
                if (file.exists()) {
                    file.deleteRecursively()
                }
                file.mkdirs()

                //获取所有下载链接

                val urls = illust.contentImages[IllustImagesType.ORIGIN]!!
                dao.update(task.copy(progress = 0f))
                val result = kotlin.runCatching {
                    val (channels, allSize) = coroutineScope {
                        val result = urls.map {
                            async(Dispatchers.IO) {
                                val resp = net.prepareGet(it).execute {
                                    it
                                }
                                resp.bodyAsChannel() to resp.headers["Content-Length"]!!.toLong()
                            }
                        }.awaitAll()

                        result.map { it.first } to result.sumOf { it.second }
                    }

                    val x = Mutex()
                    var size = 0f
                    coroutineScope {
                        channels.mapIndexed { index, source ->
                            val download = file.resolve("$index.png").sink().buffer()
                            async(Dispatchers.IO) {
                                download.use {
                                    while (!source.isClosedForRead) {
                                       source.readBuffer(1024).use {
                                           download.write(
                                               it.readBytes()
                                           )
                                           x.withLock {
                                               size += it.size.toFloat()
                                           }
                                           dao.update(task.copy(progress = size / allSize))
                                       }
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                }
                jobs.remove(illust.id.toLong())
                if (result.isFailure) {
                    dao.update(task.copy(progress = -1f))
                    result.exceptionOrNull()!!.printStackTrace()
                    postSideEffect(DownloadScreenSideEffect.Toast("${illust.title}(${illust.id})下载失败"))
                    file.deleteRecursively()
                    return@runOn
                }
                dao.update(task.copy(success = true, progress = -1f))
                postSideEffect(DownloadScreenSideEffect.Toast("${illust.title}(${illust.id})下载完成！"))
            }
        }
        return true
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
