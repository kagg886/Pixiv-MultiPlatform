package top.kagg886.pmf.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Image
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import korlibs.datastructure.mapInt
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.makeFromFileName

private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
private val GIF_HEADER_89A = "GIF89a".encodeUtf8()

class SwapChain(alloc: Bitmap.() -> Unit) {
    private val bufferA = Bitmap().apply(alloc)
    private val bufferB = Bitmap().apply(alloc)
    private val imageA = SkiaImage.makeFromBitmap(bufferA)
    private val imageB = SkiaImage.makeFromBitmap(bufferB)
    private var swap by atomic(false)
    val render
        get() = if (swap) imageA else imageB
    val decode
        get() = if (swap) bufferB else bufferA
    fun swap() = apply { swap = !swap }
}

class AnimatedSkiaImage(val codec: Codec, val scope: CoroutineScope) : Image {
    override val size = codec.imageInfo.computeMinByteSize() * 2L // We use 2 buffers
    override val width = codec.width
    override val height = codec.height
    override val shareable = true
    val delays = codec.framesInfo.mapInt { i -> i.duration }
    val wholeDuration = delays.sum()

    val swapChain = SwapChain {
        allocPixels(codec.imageInfo)
        setImmutable()
    }

    fun decode(index: Int) {
        val target = swapChain.decode
        codec.readPixels(target, index)
        target.notifyPixelsChanged()
    }

    init {
        decode(0)
    }

    private var currentDecode = 0
    fun preload(index: Int) {
        if (currentDecode != index) {
            currentDecode = index
            scope.launch(Dispatchers.Default) { decode(index) }
        }
    }

    private var invalidateTick by mutableIntStateOf(0)
    private var animationStartTime: TimeMark? = null
    private var currentDraw = -1

    override fun draw(canvas: Canvas) {
        val animationStartTime = animationStartTime ?: TimeSource.Monotonic.markNow().also { animationStartTime = it }
        val totalElapsedTimeMs = animationStartTime.elapsedNow().inWholeMilliseconds

        val currentIndex = frameIndexToDraw(totalElapsedTimeMs)
        if (currentIndex != currentDraw) {
            currentDraw = currentIndex
            swapChain.swap()
            val next = if (currentIndex == codec.frameCount - 1) 0 else currentIndex + 1
            preload(next)
        }

        canvas.drawImage(image = swapChain.render, left = 0f, top = 0f)
        invalidateTick++
    }

    fun frameIndexToDraw(totalElapsedTimeMs: Long): Int {
        if (codec.frameCount == 1) { // 只有1帧的情况返回0
            return 0
        }
        val currentIterationElapsedTimeMs = totalElapsedTimeMs % wholeDuration
        var accumulatedDuration = 0
        val index = delays.indexOfFirst { frameDuration ->
            accumulatedDuration += frameDuration
            accumulatedDuration > currentIterationElapsedTimeMs
        }
        return if (index == -1) delays.lastIndex else index
    }
}

class AnimatedSkiaImageDecoder(private val source: ImageSource) : Decoder {
    override suspend fun decode(): DecodeResult = coroutineScope {
        val codec = Codec.makeFromData(Data.makeFromFileName("${source.file()}"))
        DecodeResult(
            image = AnimatedSkiaImage(codec = codec, scope = this + Job()),
            isSampled = false,
        )
    }

    object Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return AnimatedSkiaImageDecoder(source = result.source)
        }
        fun isApplicable(source: BufferedSource) = source.rangeEquals(0, GIF_HEADER_89A) || source.rangeEquals(0, GIF_HEADER_87A)
    }
}
