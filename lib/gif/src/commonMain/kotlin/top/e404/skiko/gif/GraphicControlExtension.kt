package top.e404.skiko.gif

import okio.BufferedSink
import top.e404.skiko.gif.structure.AnimationDisposalMode

internal object GraphicControlExtension {
    private const val INTRODUCER = 0x21
    private const val LABEL = 0xF9
    private const val BLOCK_SIZE = 0x04
    private const val TERMINATOR = 0x00

    private fun block(
        buffer: BufferedSink,
        flags: Byte,
        delay: Short,
        transparencyIndex: Byte,
    ) {
        buffer.writeByte(INTRODUCER.asUnsignedByte().toInt())
        buffer.writeByte(LABEL.asUnsignedByte().toInt())
        buffer.writeByte(BLOCK_SIZE.asUnsignedByte().toInt())
        buffer.writeByte(flags.toInt())
        buffer.writeShortLe(delay.toInt())
        buffer.writeByte(transparencyIndex.toInt())
        buffer.writeByte(TERMINATOR.asUnsignedByte().toInt())
    }

    fun write(
        buffer: BufferedSink,
        disposalMethod: AnimationDisposalMode,
        userInput: Boolean,
        transparencyIndex: Int?,
        millisecond: Int,
    ) {
        // Not Interlaced Images
        var flags = 0x0000

        flags = flags or when (disposalMethod) {
            AnimationDisposalMode.UNUSED -> 0x00
            AnimationDisposalMode.KEEP -> 0x04
            AnimationDisposalMode.RESTORE_BG_COLOR -> 0x08
            AnimationDisposalMode.RESTORE_PREVIOUS -> 0x0C
        }
        if (userInput) flags = flags or 0x02
        if (transparencyIndex in 0..0xFF) flags = flags or 0x01

        block(
            buffer = buffer,
            flags = flags.asUnsignedByte(),
            delay = (millisecond / 10).asUnsignedShort(),
            transparencyIndex = (transparencyIndex ?: 0).asUnsignedByte()
        )
    }
}
