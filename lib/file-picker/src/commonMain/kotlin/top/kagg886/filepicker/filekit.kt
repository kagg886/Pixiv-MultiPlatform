package top.kagg886.filepicker

import okio.Path
import okio.Sink
import okio.Source

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/17 13:36
 * ================================================
 */

expect object FilePicker

expect suspend fun FilePicker.openFileSaver(
    suggestedName: String = "",
    extension: String? = null,
    directory: Path? = null,
): Sink?

expect suspend fun FilePicker.openFilePicker(
    ext: List<String>? = null,
    title: String? = null,
    directory: Path? = null,
): Source?
