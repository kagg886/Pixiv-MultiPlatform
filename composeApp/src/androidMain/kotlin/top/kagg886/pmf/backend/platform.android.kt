package top.kagg886.pmf.backend

import android.content.res.Resources
import android.os.Build

actual val currentPlatform: Platform by lazy {
    val float = with(Resources.getSystem().displayMetrics) {
        widthPixels / heightPixels.toFloat()
    }
    if (float > 1.0f) Platform.Android.AndroidPad(Build.VERSION.SDK_INT) else Platform.Android.AndroidPhone(Build.VERSION.SDK_INT)
}
