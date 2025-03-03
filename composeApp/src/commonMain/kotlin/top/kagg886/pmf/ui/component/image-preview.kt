package top.kagg886.pmf.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.github.panpf.sketch.LocalPlatformContext
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.cache.downloadCacheKey
import com.github.panpf.sketch.fetch.isBase64Uri
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.UriInvalidException
import com.github.panpf.sketch.util.MimeTypeMap
import com.github.panpf.sketch.util.toUri
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.rememberSketchZoomState
import io.github.vinceglb.filekit.core.FileKit
import io.ktor.http.*
import kotlinx.coroutines.launch
import okio.Buffer
import okio.ByteString.Companion.decodeBase64
import okio.Source
import okio.buffer
import okio.use
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useTempFile
import top.kagg886.pmf.copyImageToClipboard
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.ui.component.icon.Copy
import top.kagg886.pmf.ui.component.icon.Save
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source
import top.kagg886.pmf.util.transfer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ImagePreviewer(
    onDismiss: () -> Unit,
    url: List<String>,
    startIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val ctx = LocalPlatformContext.current
    val request = remember(url.hashCode()) {
        url.map {
            ImageRequest.Builder(ctx, it).build()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        val pagerState = rememberPagerState(startIndex) { url.size }

        Box {
            HorizontalPager(
                state = pagerState,
                modifier = modifier.fillMaxSize(),
            ) {
                var showBottomDialog by remember {
                    mutableStateOf(false)
                }

                if (showBottomDialog) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomDialog = false },
                    ) {
                        val snack = LocalSnackBarHost.current
                        val platform = LocalPlatformContext.current
                        val scope = rememberCoroutineScope()


                        if (currentPlatform is Platform.Desktop) {
                            SettingsMenuLink(
                                title = {
                                    Text("复制到剪贴板")
                                },
                                icon = {
                                    Icon(
                                        imageVector = Copy,
                                        null
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        when {
                                            isBase64Uri(url[pagerState.currentPage].toUri()) -> {
                                                val (_, data) = url[pagerState.currentPage].decodeBase64Uri()
                                                val source = Buffer().write(
                                                    data.decodeBase64()!!.toByteArray()
                                                )

                                                kotlin.runCatching {
                                                    copyImageToClipboard(source.readByteArray())
                                                }.onSuccess {
                                                    snack.showSnackbar("复制成功！")
                                                }.onFailure {
                                                    snack.showSnackbar("复制失败：${it.message}")
                                                }
                                            }

                                            else -> {
                                                val source =
                                                    ctx.getDownloadImage(request[pagerState.currentPage].downloadCacheKey)
                                                if (source == null) {
                                                    snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                                    return@launch
                                                }
                                                kotlin.runCatching {
                                                    copyImageToClipboard(source.buffer().readByteArray())
                                                }.onSuccess {
                                                    snack.showSnackbar("复制成功！")
                                                }.onFailure {
                                                    snack.showSnackbar("复制失败：${it.message}")
                                                }
                                            }
                                        }
//                                        val source = when {
//                                            isBase64Uri(url[pagerState.currentPage].toUri()) -> {
//                                                Buffer().write(
//                                                    url[pagerState.currentPage].decodeBase64Uri().decodeBase64()!!
//                                                        .toByteArray()
//                                                )
//                                            }
//
//                                            else -> {
//                                                ctx.getDownloadImage(request[pagerState.currentPage].downloadCacheKey)
//                                            }
//                                        }
//                                        if (source == null) {
//                                            snack.showSnackbar("文件仍在下载，请稍等片刻...")
//                                            return@launch
//                                        }
                                        showBottomDialog = false
                                    }
                                }
                            )
                        }
                        SettingsMenuLink(
                            title = {
                                Text("保存")
                            },
                            icon = {
                                Icon(Save, null)
                            },
                            onClick = {
                                scope.launch {
                                    when {
                                        isBase64Uri(url[pagerState.currentPage].toUri()) -> {
                                            val (mime, data) = url[pagerState.currentPage].decodeBase64Uri()
                                            val source = Buffer().write(
                                                data.decodeBase64()!!.toByteArray()
                                            )

                                            FileKit.saveFile(
                                                bytes = source.readByteArray(),
                                                extension = MimeTypeMap.getExtensionFromMimeType(mime) ?: "bin",
                                                baseName = Uuid.random().toHexString()
                                            )
                                        }

                                        else -> {
                                            val source =
                                                ctx.getDownloadImage(request[pagerState.currentPage].downloadCacheKey)
                                            if (source == null) {
                                                snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                                return@launch
                                            }
                                            FileKit.saveFile(
                                                bytes = source.buffer().readByteArray(),
                                                extension = "png",
                                                baseName = Url(url[pagerState.currentPage]).encodedPath.replace(
                                                    "/",
                                                    "_"
                                                )
                                            )
                                        }
                                    }

                                    showBottomDialog = false
                                }
                            }
                        )
                        if (currentPlatform is Platform.Android) {
                            SettingsMenuLink(
                                title = {
                                    Text("分享")
                                },
                                icon = {
                                    Icon(Icons.Default.Share, null)
                                },
                                onClick = {
                                    scope.launch {
                                        when {
                                            isBase64Uri(url[pagerState.currentPage].toUri()) -> {
                                                val (mime, data) = url[pagerState.currentPage].decodeBase64Uri()
                                                val source = Buffer().write(
                                                    data.decodeBase64()!!.toByteArray()
                                                )

                                                useTempFile { tmp ->
                                                    tmp.sink().buffer().use { source.transfer(it) }
                                                    shareFile(
                                                        tmp,
                                                        name = "${Uuid.random().toHexString()}.${MimeTypeMap.getExtensionFromMimeType(mime) ?: "bin"}",
                                                        mime = mime
                                                    )
                                                }
                                            }

                                            else -> {
                                                val source =
                                                    ctx.getDownloadImage(request[pagerState.currentPage].downloadCacheKey)
                                                if (source == null) {
                                                    snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                                    return@launch
                                                }
                                                useTempFile { tmp ->
                                                    tmp.sink().buffer().use { source.transfer(it) }
                                                    shareFile(tmp, mime = "image/*")
                                                }
                                            }
                                        }
//                                        val cache = SingletonSketch.get(platform).downloadCache
//                                        val cacheKey = request[pagerState.currentPage].downloadCacheKey
//                                        val file = cache.withLock(cacheKey) {
//                                            openSnapshot(cacheKey)?.use { snapshot ->
//                                                snapshot.data
//                                            }
//                                        }
//                                        if (file == null) {
//                                            snack.showSnackbar("文件仍在下载，请稍等片刻...")
//                                            return@launch
//                                        }

                                        showBottomDialog = false
                                    }
                                }
                            )
                        }
                        SettingsMenuLink(
                            title = {
                                Text("退出预览")
                            },
                            icon = {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                            },
                            onClick = onDismiss
                        )
                    }
                }

                val zoom = rememberSketchZoomState()

                SketchZoomAsyncImage(
                    request[it],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    zoomState = zoom,
                    onTap = { offset ->
                        if (offset !in zoom.zoomable.contentDisplayRect.toRect()) {
                            onDismiss()
                        }
                    },
                    onLongPress = { offset ->
                        if (offset !in zoom.zoomable.contentDisplayRect.toRect()) {
                            return@SketchZoomAsyncImage
                        }
                        showBottomDialog = true
                    }
                )

            }

            Card(
                Modifier.align(Alignment.BottomCenter).graphicsLayer {
                    this.alpha = 0.6f
                }.run {
                    if (currentPlatform is Platform.Android) {
                        //can't execute smart cast
                        if ((currentPlatform as Platform.Android).version == 35) {
                            return@run this.padding(bottom = 90.dp)
                        }
                    }
                    this.padding(bottom = 10.dp)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Spacer(Modifier.width(5.dp))
                    TextButton(onClick = {}, enabled = false) {
                        Text("${pagerState.currentPage + 1}/${url.size}")
                    }
                    Spacer(Modifier.width(5.dp))
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        enabled = pagerState.currentPage < url.size - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }

    }
}

private suspend fun PlatformContext.getDownloadImage(cacheKey: String): Source? {
    val cache = SingletonSketch.get(this).downloadCache
    val file = cache.withLock(cacheKey) {
        openSnapshot(cacheKey)?.use { snapshot ->
            snapshot.data
        }
    }
    return file?.source()
}

private fun String.decodeBase64Uri(): Pair<String, String> {
    val uri = this
    val colonSymbolIndex = uri.indexOf(":").takeIf { it != -1 }
        ?: throw UriInvalidException("Invalid base64 image uri: $uri")
    val semicolonSymbolIndex = uri.indexOf(";").takeIf { it != -1 }
        ?: throw UriInvalidException("Invalid base64 image uri: $uri")
    val commaSymbolIndex = uri.indexOf(",").takeIf { it != -1 }
        ?: throw UriInvalidException("Invalid base64 image uri: $uri")
    val mimeType = uri.substring(colonSymbolIndex + 1, semicolonSymbolIndex)
        .replace("img/", "image/")
    val data = uri.substring(commaSymbolIndex + 1)

    return mimeType to data
}
