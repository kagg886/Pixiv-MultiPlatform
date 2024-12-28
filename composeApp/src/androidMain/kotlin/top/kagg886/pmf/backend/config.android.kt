package top.kagg886.pmf.backend

import top.kagg886.pmf.PMFApplication
import java.io.File


actual val dataPath: File by lazy {
    PMFApplication.getApp().filesDir
}
actual val cachePath: File by lazy {
    PMFApplication.getApp().cacheDir
}