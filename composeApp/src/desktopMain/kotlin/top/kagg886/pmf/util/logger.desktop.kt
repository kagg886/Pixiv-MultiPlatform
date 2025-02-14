package top.kagg886.pmf.util


actual fun platformLog(level: PlatformLogLevel, msg: String, exception: Throwable?) {
    val ex = exception?.stackTraceToString()
    if (ex == null) {
        println("[$level]: $msg")
        return
    }
    println("[$level]: ${msg}\n$ex")
}