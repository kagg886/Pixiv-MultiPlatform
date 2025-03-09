package top.e404.skiko.gif

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.*
import top.e404.skiko.gif.listener.GIFMakingListener
import top.e404.skiko.gif.listener.GIFMakingStep
import top.e404.skiko.gif.structure.AnimationDisposalMode
import top.e404.skiko.gif.structure.AnimationFrameInfo
import top.e404.skiko.gif.structure.Bitmap
import top.e404.skiko.gif.structure.IRect
import top.kagg886.pmf.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GIFBuilder(val width: Int, val height: Int) {
    companion object {
        private val logger = Logger.withTag(this::class.qualifiedName!!)
        /**
         * GIF标识头
         */
        internal val GIF_HEADER = "GIF89a".encodeToByteArray()
        internal val GIF_TRAILER = ";".encodeToByteArray()
    }

    private fun header(buffer: BufferedSink) = buffer.write(GIF_HEADER)

    private fun trailer(buffer: BufferedSink) = buffer.write(GIF_TRAILER)

    var loop = 0

    /**
     * Netscape Looping Application Extension, 0 is infinite times
     * @see [ApplicationExtension.loop]
     */
    fun loop(count: Int) = apply { loop = count }

    var buffering = 0

    /**
     * Netscape Buffering Application Extension
     * @see [ApplicationExtension.buffering]
     */
    fun buffering(open: Boolean) = apply { buffering = if (open) 0x0001_0000 else 0x0000_0000 }

    var ratio = 0

    /**
     * Pixel Aspect Ratio
     * @see [LogicalScreenDescriptor.write]
     */
    fun ratio(size: Int) = apply {
        ratio = size
    }

    var global = ColorTable.Empty

    /**
     * GlobalColorTable
     * @see [OctTreeQuantizer.quantize]
     */
    fun table(bitmap: Bitmap) = apply {
        global = ColorTable(OctTreeQuantizer().quantize(bitmap, 256), true)
    }

    /**
     * GlobalColorTable
     */
    fun table(value: ColorTable) = apply {
        global = value
    }

    var options = AnimationFrameInfo(
        duration = 1000,
        disposalMethod = AnimationDisposalMode.UNUSED,
    )

    /**
     * GlobalFrameOptions
     */
    fun options(block: AnimationFrameInfo.() -> Unit): GIFBuilder = apply {
        options.apply(block)
    }

    var frames = ArrayList<Triple<() -> Bitmap, ColorTable, AnimationFrameInfo>>()

    fun frame(
        bitmap: () -> Bitmap,
        colors: ColorTable = ColorTable.Empty,
        block: AnimationFrameInfo.() -> Unit = {},
    ): GIFBuilder = apply {
        frames.add(Triple(bitmap, colors, options.copy().apply(block)))
    }

    fun frame(
        bitmap: () -> Bitmap,
        colors: ColorTable = ColorTable.Empty,
        info: AnimationFrameInfo,
    ) = apply {
        frames.add(Triple(bitmap, colors, info))
    }


    private var scope = CoroutineScope(Dispatchers.Default)
    fun scope(scope: CoroutineScope) = apply { this.scope = scope }

    private var workDir: Path? = null
    fun workDir(path: Path) = apply { workDir = path }

    private var listener: GIFMakingListener? = null
    fun progress(listener: GIFMakingListener) = apply { this.listener = listener }

    @OptIn(ExperimentalUuidApi::class)
    fun buildToSink(sink: BufferedSink) {
        val list: List<Source> = runBlocking {
            val lock = Mutex()
            var progress = 0
            frames.map { (producer, colors, info) ->
                scope.async {
                    val bitmap = withContext(scope.coroutineContext) {
                        producer()
                    }
                    val opaque = !bitmap.computeIsOpaque()
                    val table = withContext(scope.coroutineContext) {
                        when {
                            colors.exists() -> colors
                            global.exists() -> global
                            else -> ColorTable(OctTreeQuantizer().quantize(bitmap, if (opaque) 255 else 256), true)
                        }
                    }
                    val dither = withContext(scope.coroutineContext) {
                        AtkinsonDitherer.dither(bitmap, table.colors)
                    }

                    val transparency = if (opaque) table.transparency else null
                    val result = when (val work = workDir) {
                        null -> {
                            val rtn = Buffer()
                            extracted(rtn, info, transparency, bitmap, table, dither)
                            rtn
                        }

                        else -> {
                            val tmp = work.resolve(Uuid.random().toHexString())
                            tmp.parentFile()?.mkdirs()
                            tmp.createNewFile()

                            tmp.sink().buffer().use { rtn ->
                                extracted(rtn, info, transparency, bitmap, table, dither)
                                rtn.flush()
                            }

                            tmp.source()
                        }
                    }
                    lock.withLock {
                        listener?.onProgress(GIFMakingStep.CompressImage(progress++, frames.size))
                    }
                    result
                }
            }.awaitAll()
        }

        header(sink)
        LogicalScreenDescriptor.write(sink, width, height, global, ratio)
        if (loop >= 0) ApplicationExtension.loop(sink, loop)
        if (buffering > 0) ApplicationExtension.buffering(sink, buffering)

        list.forEachIndexed { index, buffer ->
            logger.i("Writing Frame: $index")
            buffer.buffer().use { it.transfer(sink) }
            logger.i("Writing Frame: $index done.")
            listener?.onProgress(GIFMakingStep.WritingData(index, frames.size))
        }
        trailer(sink)
        sink.flush()
        if (workDir != null) {
            workDir!!.deleteRecursively()
        }
    }

    private suspend fun extracted(
        rtn: BufferedSink,
        info: AnimationFrameInfo,
        transparency: Int?,
        bitmap: Bitmap,
        table: ColorTable,
        dither: IntArray
    ) {
        withContext(scope.coroutineContext) {
            GraphicControlExtension.write(
                rtn,
                info.disposalMethod,
                false,
                transparency,
                info.duration
            )
        }
        val descBuf = withContext(scope.coroutineContext) {
            ImageDescriptor.toBuffer(
                IRect.makeXYWH(0, 0, bitmap.width, bitmap.height),
                table,
                table !== global,
                dither
            )
        }
        descBuf.use {
            rtn.write(it.readByteArray())
        }
    }
}
