package top.kagg886.pmf.backend

import top.kagg886.pmf.PMFApplication
import java.io.File


actual val rootPath: File by lazy {
    PMFApplication.getApp().filesDir
}