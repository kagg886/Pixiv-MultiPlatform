package top.e404.skiko.gif

import okio.*
import top.e404.skiko.gif.structure.IRect

internal object ImageDescriptor {
    private const val SEPARATOR = 0x002C
    private const val TERMINATOR = 0x0000

    private fun block(
        buffer: BufferedSink,
        left: Short,
        top: Short,
        width: Short,
        height: Short,
        flags: Byte,
    ) {
        buffer.writeByte(SEPARATOR.asUnsignedByte().toInt())
        buffer.writeShortLe(left.toInt())
        buffer.writeShortLe(top.toInt())
        buffer.writeShortLe(width.toInt())
        buffer.writeShortLe(height.toInt())
        buffer.writeByte(flags.toInt())
    }

    private fun data(
        buffer: BufferedSink,
        min: Int,
        data: BufferedSource,
    ) {
        buffer.writeByte(min.asUnsignedByte().toInt())


        val buf = ByteArray(255)
        var len:Int
        while (data.read(buf).also { len = it } != -1) {
            buffer.writeByte(len.asUnsignedByte().toInt())
            buffer.write(buf, 0, len)
        }
//        for (index in data.indices step 255) {
//            val size = minOf(data.size - index, 255)
//            buffer.writeByte(size.asUnsignedByte().toInt()) // 1 Byte
//            buffer.write(data, index, size)
//        }
    }

    fun toBuffer(
        rect: IRect,
        table: ColorTable,
        local: Boolean,
        image: IntArray
    ): Buffer {
        val (min, lzw) = LZWEncoder(table, image).encode()

        // Not Interlaced Images
        var flags = 0x00

        if (local) {
            flags = 0x80 or table.size()
            if (table.sort) {
                flags = flags or 0x10
            }
        }

        val buffer = Buffer()

        block(
            buffer = buffer,
            left = rect.left.asUnsignedShort(),
            top = rect.top.asUnsignedShort(),
            width = rect.width.asUnsignedShort(),
            height = rect.height.asUnsignedShort(),
            flags = flags.asUnsignedByte()
        ) // 10

        if (local) table.write(buffer) // (colors.capacity() - colors.size) * 3 + 3

        Buffer().write(lzw).use {
            data(buffer, min, it)
        }

        buffer.writeByte(TERMINATOR.asUnsignedByte().toInt())

        return buffer
    }
}
