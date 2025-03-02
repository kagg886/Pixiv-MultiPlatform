package top.kagg886.pmf.util

import okio.BufferedSink

inline fun BufferedSink.writeByte(byte: Byte) = writeByte(byte.toInt())

inline fun BufferedSink.writeShort(short: Short) = writeShort(short.toInt())
inline fun BufferedSink.writeShortLe(short: Short) = writeShortLe(short.toInt())
