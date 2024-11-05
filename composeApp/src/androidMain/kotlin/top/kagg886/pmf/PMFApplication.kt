package top.kagg886.pmf

import android.annotation.SuppressLint
import android.app.Application
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import io.github.vinceglb.filekit.core.FileKit

class PMFApplication : Application(), SingletonSketch.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin0()
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
}