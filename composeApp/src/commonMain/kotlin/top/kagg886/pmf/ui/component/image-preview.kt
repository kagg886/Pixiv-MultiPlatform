package top.kagg886.pmf.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import okio.Buffer
import okio.buffer
import okio.use
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useTempFile
import top.kagg886.pmf.copyImageToClipboard
import top.kagg886.pmf.copy_to_clipboard
import top.kagg886.pmf.copy_to_clipboard_failed
import top.kagg886.pmf.copy_to_clipboard_success
import top.kagg886.pmf.exit_preview
import top.kagg886.pmf.file_was_downloading
import top.kagg886.pmf.save
import top.kagg886.pmf.share
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.ui.component.icon.Copy
import top.kagg886.pmf.ui.component.icon.Save
import top.kagg886.pmf.util.UGOIRA_SCHEME
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source
import top.kagg886.pmf.util.transfer

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
                                Text(stringResource(Res.string.copy_to_clipboard))
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
                                        snack.showSnackbar(getString(Res.string.file_was_downloading))
                                    } else {
                                        runCatching {
                                            copyImageToClipboard(bytes)
                                        }.onSuccess {
                                            snack.showSnackbar(getString(Res.string.copy_to_clipboard_success))
                                        }.onFailure {
                                            logger.w("copy image to clipboard failed", it)
                                            snack.showSnackbar(getString(Res.string.copy_to_clipboard_failed))
                                        }
                                    }
                                    showBottomDialog = false
                                }
                            },
                        )
                    }
                    SettingsMenuLink(
                        title = {
                            Text(stringResource(Res.string.save))
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
                                    snack.showSnackbar(getString(Res.string.file_was_downloading))
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
                                Text(stringResource(Res.string.share))
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
                                        getString(Res.string.file_was_downloading)
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
                            Text(stringResource(Res.string.exit_preview))
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
