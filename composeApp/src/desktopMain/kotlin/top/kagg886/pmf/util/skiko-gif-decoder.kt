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

    // Well, we are only allowed to draw [SkImage] **not** [SKBitmap] on [SkCanvas]
    // But we are only allowed to decode a [SkCodec] frame to [SKBitmap]
    // If we reuse [SKBitmap]'s memory in decode process, [SKBitmap] is mutable
    // Convert it to [SkImage] using [SkiaImage.makeFromBitmap] will copy underlying pixels
    // That's waste, and any update(i.e. reuse for decode) on bitmap will not reflect on [SkImage]
    // If we use [SKBitmap] as immutable, we must allocate many many instances to decode.
    //
    // Let's save out memory, we pretend [SKBitmap] is immutable by calling [setImmutable()], so
    // [SkiaImage.makeFromBitmap] will let [SkImage] share [SKBitmap]'s pixels memory
    // Then we force mutate [SKBitmap](i.e. reuse to decode), well, redraw the same [SkImage]
    // instance doesn't make a difference? Call [notifyPixelsChanged()] after decode(update underlying memory).

    // 我们只能在 [SkCanvas] 上绘制 [SkImage] **而不是** [SKBitmap]
    // 但我们同时只能把 [SkCodec] 解码到 [SKBitmap]
    // 如果我们在解码过程中重用 [SKBitmap] 的内存，那我们需要要求 [SKBitmap] 是可变的
    // 使用 [SkiaImage.makeFromBitmap] 将可变的位图转换为 [SkImage] 会复制底层像素
    // 太浪费了! 并且位图内存上的任何更新（在解码过程中重用它）都不会更新在 [SkImage] 上
    // 如果我们把 [SKBitmap] 用作不可变的，我们必须分配很多实例来解码。
    //
    // 省省吧，我们通过调用 [setImmutable()] 假装 [SKBitmap] 是不可变的，所以
    // [SkiaImage.makeFromBitmap] 会让 [SkImage] 共享 [SKBitmap] 的像素内存
    // 然后我们强制更新 [SKBitmap]（即重新用于解码），但是重新使用相同的 [SkImage] 实例进行绘制
    // 没有区别？我们忘了更新像素内存后调用 [notifyPixelsChanged()]。
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
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        DecodeResult(
            image = AnimatedSkiaImage(
                codec = codec,
                scope = this + Job(),
            ),
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
