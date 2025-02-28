package top.kagg886.pmf.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.*
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.rememberRingProgressPainter
import com.github.panpf.sketch.request.ComposableImageRequest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.getResourceUri

@Deprecated(message = "release模式下存在未知bug导致无法加载资源",replaceWith = ReplaceWith(""))
@OptIn(InternalResourceApi::class)
@Composable
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun painterResource(drawableResource: DrawableResource): Painter {
    return rememberAsyncImagePainter(
        request = ComposableImageRequest(LocalPlatformContext.current,getResourceUri(drawableResource.items.toList()[0].path))
    )
}

@Composable
fun ProgressedAsyncImage(
    url: String?,
    modifier: Modifier = Modifier,
    state:AsyncImageState = rememberAsyncImageState(),
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    clipToBounds: Boolean = true,
) {
    val progressPainter = rememberRingProgressPainter()
    AsyncImage(
        request = ComposableImageRequest(url) {
            crossfade()
            resizeOnDraw()
        },
        contentDescription = null,
        state = state,
        modifier = modifier.progressIndicator(state, progressPainter),
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
        clipToBounds = clipToBounds
    )
}
