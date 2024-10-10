package top.kagg886.pmf.ui.route.main.download

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.headersContentLength
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.DownloadItem
import top.kagg886.pmf.backend.rootPath
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.util.ignoreSSL

class DownloadScreenModel : ContainerHost<DownloadScreenState, DownloadScreenSideEffect>, ViewModel(), ScreenModel,
    KoinComponent {
    private val database by inject<AppDatabase>()

    private val net = OkHttpClient.Builder().apply {
        ignoreSSL()
        addNetworkInterceptor {
            it.proceed(it.request().newBuilder().apply {
                header("Referer", "https://www.pixiv.net/")
            }.build())
        }
    }.build()

    private val flowMap: MutableMap<Long, MutableStateFlow<Float>> = mutableMapOf()

    fun downloadFlow(id: Long) = flowMap.getOrPut(id) {
        MutableStateFlow(-1f)
    }

    @OptIn(OrbitExperimental::class)
    fun startDownload(item: DownloadItem) = intent {
        runOn<DownloadScreenState.Loaded> {
            val name = item.url.toHttpUrl().encodedPathSegments.last()

            database.downloadDAO().update(

                item = item.copy(success = false)
            )
            val resp = kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    val body = net.newCall(
                        Request.Builder()
                            .url(item.url)
                            .build()
                    ).execute()

                    body.body!!.byteStream() to body.headersContentLength().toFloat()
                }
            }
            if (resp.isFailure) {
                postSideEffect(DownloadScreenSideEffect.Toast("下载失败"))
                return@runOn
            }
            val (source, length) = resp.getOrThrow()

            val download = rootPath.resolve("download").resolve(name).apply {
                if (exists()) {
                    delete()
                }
                parentFile?.mkdirs()
                createNewFile()
            }.outputStream()

            download.use {
                downloadFlow(item.id).value = 0f
                withContext(Dispatchers.IO) {
                    var size: Long = 0
                    val buf = ByteArray(1024)
                    var len: Int;
                    while (source.read(buf).also { len = it } != -1) {
                        download.write(buf, 0, len)
                        size += len
                        val progress = size / length
                        flowMap[item.id]?.emit(progress)
                    }
                }
                database.downloadDAO().update(item.copy(success = true))
                downloadFlow(item.id).value = -1f
                postSideEffect(DownloadScreenSideEffect.Toast("下载完成"))

            }
        }

    }

    @OptIn(OrbitExperimental::class)
    fun startDownload(item: String) = intent {
        runOn<DownloadScreenState.Loaded> {
            val newId = database.downloadDAO().insert(
                DownloadItem(
                    0L,
                    item,
                    false
                )
            )
            val data = database.downloadDAO().find(newId)!!
            startDownload(data)
        }
    }

    override val container: Container<DownloadScreenState, DownloadScreenSideEffect> =
        container(DownloadScreenState.Loading) {
            val data = database.downloadDAO().all()
            for (i in database.downloadDAO().allSuspend()) {
                val name = i.url.toHttpUrl().encodedPathSegments.last()
                val download = rootPath.resolve("download").resolve(name)
                if (!download.exists()) {
                    database.downloadDAO().update(i.copy(success = false))
                }
            }
            reduce {
                DownloadScreenState.Loaded(data)
            }
        }

}

sealed class DownloadScreenState {
    data object Loading : DownloadScreenState()
    data class Loaded(val illust: Flow<List<DownloadItem>>) : DownloadScreenState()
}

sealed class DownloadScreenSideEffect {
    data class Toast(val msg: String) : DownloadScreenSideEffect()
}
