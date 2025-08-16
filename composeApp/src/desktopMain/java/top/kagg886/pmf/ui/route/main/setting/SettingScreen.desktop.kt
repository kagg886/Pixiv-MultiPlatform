package top.kagg886.pmf.ui.route.main.setting

import top.kagg886.filepicker.FilePicker
import top.kagg886.filepicker.openFolderPicker
import top.kagg886.pmf.util.absolutePath

actual suspend fun getDownloadRootPath(): String? = FilePicker.openFolderPicker()?.absolutePath()?.toString()
