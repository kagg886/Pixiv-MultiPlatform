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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.Uri
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.alorma.compose.settings.ui.SettingsMenuLink
import io.github.vinceglb.filekit.core.FileKit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import okio.Buffer
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
import top.kagg886.pmf.util.UGOIRA_SCHEME
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source
import top.kagg886.pmf.util.transfer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun ImagePreviewer(
    onDismiss: () -> Unit,
    data: List<Uri>,
    startIndex: Int = 0,
    modifier: Modifier = Modifier,
) = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
) {
    val pagerState = rememberPagerState(startIndex) { data.size }
    val ctx = LocalPlatformContext.current
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
                    val scope = rememberCoroutineScope()
                    val snack = LocalSnackBarHost.current
                    if (currentPlatform is Platform.Desktop) {
                        SettingsMenuLink(
                            title = {
                                Text("复制到剪贴板")
                            },
                            icon = {
                                Icon(
                                    imageVector = Copy,
                                    null,
                                )
                            },
                            onClick = {
                                scope.launch {
                                    val key = data[pagerState.currentPage].toString()
                                    val bytes = ctx.readBytes(key)
                                    if (bytes == null) {
                                        snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                    } else {
                                        runCatching {
                                            copyImageToClipboard(bytes)
                                        }.onSuccess {
                                            snack.showSnackbar("复制成功！")
                                        }.onFailure {
                                            snack.showSnackbar("复制失败：${it.message}")
                                        }
                                    }
                                    showBottomDialog = false
                                }
                            },
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
                                val key = data[pagerState.currentPage].toString()
                                val isGif = key.startsWith(UGOIRA_SCHEME)
                                val bytes = ctx.readBytes(key)
                                if (bytes == null) {
                                    snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                } else {
                                    FileKit.saveFile(
                                        bytes = bytes,
                                        extension = if (isGif) "gif" else "png",
                                        baseName = Uuid.random().toHexString(),
                                    )
                                }
                                showBottomDialog = false
                            }
                        },
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
                                    val key = data[pagerState.currentPage].toString()
                                    val isGif = key.startsWith(UGOIRA_SCHEME)
                                    val bytes = ctx.readBytes(key)
                                    if (bytes == null) {
                                        snack.showSnackbar("文件仍在下载，请稍等片刻...")
                                    } else {
                                        val source = Buffer().write(bytes)
                                        useTempFile { tmp ->
                                            tmp.sink().buffer().use { source.transfer(it) }
                                            if (isGif) {
                                                shareFile(
                                                    tmp,
                                                    name = "${Uuid.random().toHexString()}.gif",
                                                    mime = "image/gif",
                                                )
                                            } else {
                                                shareFile(tmp, mime = "image/*")
                                            }
                                        }
                                    }
                                }
                                showBottomDialog = false
                            },
                        )
                    }
                    SettingsMenuLink(
                        title = {
                            Text("退出预览")
                        },
                        icon = {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                        },
                        onClick = onDismiss,
                    )
                }
            }

            val state = rememberZoomableState()
            AsyncImage(
                model = data[it],
                contentDescription = null,
                onSuccess = { s ->
                    val size = Size(s.result.image.width.toFloat(), s.result.image.height.toFloat())
                    val location = ZoomableContentLocation.scaledToFitAndCenterAligned(size)
                    state.setContentLocation(location)
                },
                modifier = Modifier.fillMaxSize().zoomable(
                    state = state,
                    onClick = { offset ->
                        if (offset !in state.transformedContentBounds) onDismiss()
                    },
                    onLongClick = { offset ->
                        if (offset in state.transformedContentBounds) showBottomDialog = true
                    },
                ),
            )
        }

        Card(
            Modifier.align(Alignment.BottomCenter).graphicsLayer {
                this.alpha = 0.6f
            }.run {
                if (currentPlatform is Platform.Android) {
                    // can't execute smart cast
                    if ((currentPlatform as Platform.Android).version == 35) {
                        return@run this.padding(bottom = 90.dp)
                    }
                }
                this.padding(bottom = 10.dp)
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val scope = rememberCoroutineScope()
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Spacer(Modifier.width(5.dp))
                TextButton(onClick = {}, enabled = false) {
                    Text("${pagerState.currentPage + 1}/${data.size}")
                }
                Spacer(Modifier.width(5.dp))
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    enabled = pagerState.currentPage < data.size - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

private fun PlatformContext.readBytes(key: String) = run {
    val coil = SingletonImageLoader.get(this).diskCache!!
    coil.openSnapshot(key)?.use { snapshot ->
        snapshot.data.source().buffer().use { src ->
            src.readByteArray()
        }
    }
}
