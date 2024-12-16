package top.kagg886.pmf.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.rememberMaskProgressPainter
import com.github.panpf.sketch.painter.rememberRingProgressPainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest

@Composable
fun ProgressedAsyncImage(
    url: String?,
    modifier: Modifier = Modifier,

    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    clipToBounds: Boolean = true,
) {
    val progressPainter = rememberRingProgressPainter()
    val imageState = rememberAsyncImageState()
    AsyncImage(
        request = ComposableImageRequest(url) {
            crossfade()
        },
        contentDescription = null,
        state = imageState,
        modifier = modifier
            .progressIndicator(imageState, progressPainter),
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
        clipToBounds = clipToBounds
    )
}