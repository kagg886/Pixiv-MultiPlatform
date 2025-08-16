package top.kagg886.pmf.ui.route.main.setting

import android.content.Intent
import android.net.Uri
import top.kagg886.filepicker.FilePicker
import top.kagg886.filepicker.openFolderPicker
import top.kagg886.pmf.PMFApplication

actual suspend fun getDownloadRootPath(): String? = FilePicker.openFolderPicker()?.toString()
