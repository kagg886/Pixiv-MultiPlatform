package top.kagg886.pmf.backend

import android.content.res.Resources

actual val currentPlatform: Platform by lazy {
    val float = with(Resources.getSystem().displayMetrics) {
        widthPixels / heightPixels.toFloat()
    }
    if (float > 1.0f) Platform.Android.AndroidPad else Platform.Android.AndroidPhone
}