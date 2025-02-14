package top.kagg886.pmf.util

import android.util.Log
import top.kagg886.pmf.util.PlatformLogLevel.*


actual fun platformLog(level: PlatformLogLevel, msg: String, exception: Throwable?) {
    when(level) {
        DEBUG -> Log.d("PlatformLogger",msg,exception)
        INFO -> Log.i("PlatformLogger",msg,exception)
        WARN -> Log.w("PlatformLogger",msg,exception)
        ERROR -> Log.e("PlatformLogger",msg,exception)
    }
}