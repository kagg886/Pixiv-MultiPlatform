package top.e404.skiko.gif

import okio.BufferedSink
internal object LogicalScreenDescriptor {

    private fun block(
        buffer: BufferedSink,
        width: Short,
        height: Short,
        flags: Byte,
        backgroundColorIndex: Byte,
        pixelAspectRatio: Byte,
    ) {
        buffer.writeShortLe(width.toInt())
        buffer.writeShortLe(height.toInt())
        buffer.writeByte(flags.toInt())
        buffer.writeByte(backgroundColorIndex.toInt())
        buffer.writeByte(pixelAspectRatio.toInt())
//        buffer.putShort(width)
//        buffer.putShort(height)
//        buffer.put(flags)
//        buffer.put(backgroundColorIndex)
//        buffer.put(pixelAspectRatio)
    }

    fun write(
        buffer: BufferedSink,
        width: Int,
        height: Int,
        table: ColorTable,
        ratio: Int,
    ) {
        // Color Resolution Use 7
        var flags = 0x70
        if (table.exists()) flags = flags or 0x80 or table.size()
        if (table.sort) flags = flags or 0x08
        block(
            buffer = buffer,
            width = width.asUnsignedShort(),
            height = height.asUnsignedShort(),
            flags = flags.asUnsignedByte(),
            backgroundColorIndex = table.background.asUnsignedByte(),
            pixelAspectRatio = ratio.asUnsignedByte()
        )

        table.write(buffer)
    }
}
