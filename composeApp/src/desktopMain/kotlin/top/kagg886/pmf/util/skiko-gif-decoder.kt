package top.kagg886.pmf.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import coil3.Image
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
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
import org.jetbrains.skia.AnimationFrameInfo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as SkiaImage

class AnimatedSkiaImage(
    private val codec: Codec,
    private val coroutineScope: CoroutineScope,
    private val timeSource: TimeSource,
) : Image {

    override val size: Long
        get() {
            var size = codec.imageInfo.computeMinByteSize().toLong()
            if (size <= 0L) size = 4L * codec.width * codec.height
            return size.coerceAtLeast(0)
        }

    override val width: Int
        get() = codec.width

    override val height: Int
        get() = codec.height

    override val shareable: Boolean
        get() = false

    /**
     * The duration of each frame, in order, in milliseconds.
     */
    val frameDurationsMs: List<Int> by lazy {
        codec.framesInfo.map { it.safeFrameDuration }
    }

    /**
     * The total duration of a single iteration of the animation in milliseconds.
     */
    val singleIterationDurationMs: Int by lazy {
        frameDurationsMs.sum()
    }

    private val bitmapA = Bitmap().apply { allocPixels(codec.imageInfo) }
    private val bitmapB = Bitmap().apply { allocPixels(codec.imageInfo) }
    private var current by atomic(false)

    fun decode(index: Int) = coroutineScope.launch(Dispatchers.Default) {
        val to = if (current) bitmapA else bitmapB
        codec.readPixels(to, index)
        current = !current
    }

    init {
        decode(0)
    }

    private var invalidateTick by mutableIntStateOf(0)

    private var animationStartTime: TimeMark? = null

    override fun draw(canvas: Canvas) {
        if (codec.frameCount == 0) {
            return
        }

        if (codec.frameCount == 1) {
            canvas.drawImage(
                image = SkiaImage.makeFromBitmap(bitmapA),
                left = 0f,
                top = 0f,
            )
            return
        }

        val animationStartTime = animationStartTime
            ?: timeSource.markNow().also { animationStartTime = it }

        val totalElapsedTimeMs = animationStartTime.elapsedNow().inWholeMilliseconds

        val frameIndexToDraw = getFrameIndexToDraw(
            frameDurationsMs = frameDurationsMs,
            singleIterationDurationMs = singleIterationDurationMs,
            totalElapsedTimeMs = totalElapsedTimeMs,
        )

        canvas.drawImage(
            image = SkiaImage.makeFromBitmap(if (!current) bitmapA else bitmapB),
            left = 0f,
            top = 0f,
        )

        val nextFrameIndex = if (frameIndexToDraw == codec.frameCount - 1) 0 else frameIndexToDraw + 1
        decode(nextFrameIndex)
        invalidateTick++
    }

    private fun getFrameIndexToDraw(
        frameDurationsMs: List<Int>,
        singleIterationDurationMs: Int,
        totalElapsedTimeMs: Long,
    ): Int {
        val currentIterationElapsedTimeMs = totalElapsedTimeMs % singleIterationDurationMs
        return getFrameIndexToDrawWithinIteration(
            frameDurationsMs = frameDurationsMs,
            elapsedTimeMs = currentIterationElapsedTimeMs,
        )
    }

    private fun getFrameIndexToDrawWithinIteration(
        frameDurationsMs: List<Int>,
        elapsedTimeMs: Long,
    ): Int {
        var accumulatedDuration = 0

        for ((index, frameDuration) in frameDurationsMs.withIndex()) {
            if (accumulatedDuration > elapsedTimeMs) {
                return (index - 1).coerceAtLeast(0)
            }
            accumulatedDuration += frameDuration
        }
        return frameDurationsMs.lastIndex
    }
}

private val AnimationFrameInfo.safeFrameDuration: Int
    get() = duration.let { if (it <= 0) DEFAULT_FRAME_DURATION else it }

private const val DEFAULT_FRAME_DURATION = 100

class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val timeSource: TimeSource,
) : Decoder {

    override suspend fun decode(): DecodeResult = coroutineScope {
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        DecodeResult(
            image = AnimatedSkiaImage(
                codec = codec,
                coroutineScope = this + Job(),
                timeSource = timeSource,
            ),
            isSampled = false,
        )
    }

    class Factory(private val timeSource: TimeSource = TimeSource.Monotonic) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return AnimatedSkiaImageDecoder(
                source = result.source,
                timeSource = timeSource,
            )
        }

        private fun isApplicable(source: BufferedSource): Boolean = DecodeUtils.isGif(source)
    }
}

private val GIF_HEADER_87A = "GIF87a".encodeUtf8()
private val GIF_HEADER_89A = "GIF89a".encodeUtf8()

private fun DecodeUtils.isGif(source: BufferedSource): Boolean = source.rangeEquals(0, GIF_HEADER_89A) || source.rangeEquals(0, GIF_HEADER_87A)
