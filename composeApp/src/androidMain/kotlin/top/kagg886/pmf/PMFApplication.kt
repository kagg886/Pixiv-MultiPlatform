package top.kagg886.pmf

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import top.kagg886.pmf.util.initFileLogger
import kotlin.concurrent.thread

class PMFApplication : Application(), SingletonSketch.Factory, Thread.UncaughtExceptionHandler {

    override fun onCreate() {
        super.onCreate()
        setupEnv()

        Thread.setDefaultUncaughtExceptionHandler(this)
        Handler(Looper.getMainLooper()).post {
            try {
                while (true) {
                    Looper.loop()
                }
            } catch (e: Throwable) {
                uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    override fun createSketch(context: PlatformContext): Sketch {
        return Sketch.Builder(context).applyCustomSketchConfig()
    }

    companion object {
        @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
        fun getApp(): PMFApplication {
            var application: Application? = null
            try {
                val atClass = Class.forName("android.app.ActivityThread")
                val currentApplicationMethod = atClass.getDeclaredMethod("currentApplication")
                currentApplicationMethod.isAccessible = true
                application = currentApplicationMethod.invoke(null) as Application
            } catch (ignored: Exception) {
            }
            if (application != null) return application as PMFApplication
            try {
                val atClass = Class.forName("android.app.AppGlobals")
                val currentApplicationMethod = atClass.getDeclaredMethod("getInitialApplication")
                currentApplicationMethod.isAccessible = true
                application = currentApplicationMethod.invoke(null) as Application
            } catch (ignored: Exception) {
            }
            return application as PMFApplication
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e("uncaughtException", "App crashed", e)
        thread {
            val i = Intent(this, CrashActivity::class.java)
            i.putExtra("exceptions", e.stackTraceToString())
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
//            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
