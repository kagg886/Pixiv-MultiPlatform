package top.kagg886.pmf.backend

import okio.Path
import okio.Path.Companion.toOkioPath
import top.kagg886.pmf.PMFApplication

actual val dataPath: Path by lazy {
    PMFApplication.getApp().filesDir.toOkioPath()
}
actual val cachePath: Path by lazy {
    PMFApplication.getApp().cacheDir.toOkioPath()
}
