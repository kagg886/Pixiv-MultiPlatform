package top.e404.skiko.gif

import kotlinx.coroutines.*
import okio.Buffer
import okio.BufferedSink
import okio.use
import top.e404.skiko.gif.structure.*

class GIFBuilder(val width: Int, val height: Int) {
    companion object {
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
        frameRect = IRect.makeXYWH(0, 0, 0, 0)
    )

    /**
     * GlobalFrameOptions
     */
    fun options(block: AnimationFrameInfo.() -> Unit): GIFBuilder = apply {
        options.apply(block)
    }

    var frames = ArrayList<Triple<Bitmap, ColorTable, AnimationFrameInfo>>()

    fun frame(
        bitmap: Bitmap,
        colors: ColorTable = ColorTable.Empty,
        block: AnimationFrameInfo.() -> Unit = {},
    ): GIFBuilder = apply {
        val rect = IRect.makeXYWH(0, 0, bitmap.width, bitmap.height)
        frames.add(Triple(bitmap, colors, options.copy(frameRect = rect).apply(block)))
    }

    fun frame(
        bitmap: Bitmap,
        colors: ColorTable = ColorTable.Empty,
        info: AnimationFrameInfo,
    ) = apply {
        frames.add(Triple(bitmap, colors, info))
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    fun buildToSink(sink:BufferedSink) {
        val list = runBlocking {
            frames.map { (bitmap, colors, info) ->
                CoroutineScope(Dispatchers.Default).async {
                    val opaque = !bitmap.computeIsOpaque()
                    val table = when {
                        colors.exists() -> colors
                        global.exists() -> global
                        else -> ColorTable(OctTreeQuantizer().quantize(bitmap, if (opaque) 255 else 256), true)
                    }
                    val transparency = if (opaque) table.transparency else null
                    val result = AtkinsonDitherer.dither(bitmap, table.colors)


                    val rtn = Buffer()
                    GraphicControlExtension.write(
                        rtn,
                        info.disposalMethod,
                        false,
                        transparency,
                        info.duration
                    )

                    val descBuf = ImageDescriptor.toBuffer(info.frameRect, table, table !== global, result)
                    descBuf.use {
                        rtn.write(it.readByteArray())
                    }
                    rtn
                }
            }.awaitAll()
        }


        header(sink)
        LogicalScreenDescriptor.write(sink, width, height, global, ratio)
        if (loop >= 0) ApplicationExtension.loop(sink, loop)
        if (buffering > 0) ApplicationExtension.buffering(sink, buffering)

        list.forEach { buffer->
            buffer.use { sink.write(it.readByteArray()) }
        }
        trailer(sink)

    }
}
