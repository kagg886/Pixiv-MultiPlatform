package top.kagg886.pmf

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlin.concurrent.thread

class PMFApplication : Application(), SingletonImageLoader.Factory, Thread.UncaughtExceptionHandler {

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
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
//            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(context).applyCustomConfig().build()
}
