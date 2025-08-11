package top.kagg886.filepicker.internal

internal class NativeFilePicker {
    external fun openFileSaver(suggestedName: String, extension: String?, directory: String?): Long
    external fun awaitFileSaver(ptr: Long): String?
    external fun openFilePicker(ext: Array<String>?, title: String?, directory: String?): Long
    external fun awaitFilePicker(ptr: Long): String?

    external fun openDictionaryPicker(title: String?, directory: String?): Long
    external fun awaitDictionaryPicker(ptr: Long): String?
}
