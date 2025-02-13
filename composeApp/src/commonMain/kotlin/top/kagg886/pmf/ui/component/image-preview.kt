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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.github.panpf.sketch.LocalPlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.cache.downloadCacheKey
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.rememberSketchZoomState
import io.github.vinceglb.filekit.core.FileKit
import io.ktor.http.*
import kotlinx.coroutines.launch
import okio.buffer
import okio.use
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.copyImageToClipboard
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.ui.component.icon.Copy
import top.kagg886.pmf.ui.component.icon.Save
import top.kagg886.pmf.util.source

@OptIn(ExperimentalMaterial3Api::class)
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
                                        val cache = SingletonSketch.get(platform).downloadCache
                                        val cacheKey = request[pagerState.currentPage].downloadCacheKey
                                        val file = cache.withLock(cacheKey) {
                                            openSnapshot(cacheKey)?.use { snapshot ->
                                                snapshot.data
                                            }
                                        }
                                        if (file == null) {
                                            snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                            return@launch
                                        }
                                        kotlin.runCatching {
                                            copyImageToClipboard(file.source().buffer().readByteArray())
                                        }.onSuccess {
                                            snack.showSnackbar("复制成功！")
                                        }.onFailure {
                                            snack.showSnackbar("复制失败：${it.message}")
                                        }
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
                                    val cache = SingletonSketch.get(platform).downloadCache
                                    val cacheKey = request[pagerState.currentPage].downloadCacheKey
                                    val file = cache.withLock(cacheKey) {
                                        openSnapshot(cacheKey)?.use { snapshot ->
                                            snapshot.data
                                        }
                                    }
                                    if (file == null) {
                                        snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                        return@launch
                                    }
                                    FileKit.saveFile(
                                        bytes = file.source().buffer().readByteArray(),
                                        extension = "png",
                                        baseName = Url(url[pagerState.currentPage]).encodedPath.replace("/", "_")
                                    )
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
                                        val cache = SingletonSketch.get(platform).downloadCache
                                        val cacheKey = request[pagerState.currentPage].downloadCacheKey
                                        val file = cache.withLock(cacheKey) {
                                            openSnapshot(cacheKey)?.use { snapshot ->
                                                snapshot.data
                                            }
                                        }
                                        if (file == null) {
                                            snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                            return@launch
                                        }
                                        shareFile(file, mime = "image/*")
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

            Card(Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp)) {
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