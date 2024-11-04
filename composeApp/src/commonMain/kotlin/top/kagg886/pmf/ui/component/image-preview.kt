package top.kagg886.pmf.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewer(
    onDismiss: () -> Unit,
    url: List<String>,
    startIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val pagerState = rememberPagerState(startIndex) { url.size }
        Box {
            HorizontalPager(
                state = pagerState,
                modifier = modifier.fillMaxSize()
            ) {
                SketchZoomAsyncImage(
                    url[it],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            val scope = rememberCoroutineScope()

            if (url.size > 1) {
                Card(modifier = Modifier.padding(10.dp).align(Alignment.BottomCenter)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        TextButton(onClick = {}) {
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

            IconButton(
                onClick = onDismiss
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
    }

}