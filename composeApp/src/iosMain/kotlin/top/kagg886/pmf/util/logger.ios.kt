package top.kagg886.pmf.util

import platform.Foundation.NSLog
import top.kagg886.pmf.util.PlatformLogLevel.*

actual fun platformLog(level: PlatformLogLevel, msg: String, exception: Throwable?) {
    val ex = exception?.stackTraceToString()
    if (ex==null) {
        NSLog("[$level]: $msg")
        return
    }

    NSLog("[$level]: ${msg}\n$ex")
}