package top.kagg886.filepicker.internal

import okio.Path

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/17 13:59
 * ================================================
 */
internal class NativeFilePicker {
    external fun openFileSaver(suggestedName: String, extension: String?, directory: String?): String?
    external fun openFilePicker(ext: Array<String>?, title: String?, directory: String?): String?
}
