package top.kagg886.pmf.util


enum class PlatformLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

expect fun platformLog(level: PlatformLogLevel,msg:String,exception: Throwable? = null)