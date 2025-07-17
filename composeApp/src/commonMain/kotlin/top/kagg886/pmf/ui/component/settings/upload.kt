package top.kagg886.pmf.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold
import kotlinx.coroutines.launch
import okio.BufferedSource
import okio.buffer
import okio.use
import top.kagg886.filepicker.FilePicker
import top.kagg886.filepicker.openFilePicker

@Composable
fun SettingsFileUpload(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: @Composable () -> Unit,
    extensions: List<String> = listOf(),
    subTitle: @Composable () -> Unit = {},
    onValueChange: (ByteArray) -> Unit,
) {
    val scope = rememberCoroutineScope()
//    val launcher = rememberFilePickerLauncher(type = FileKitType.File(extensions)) {
//        if (it != null) {
//            scope.launch {
//                onValueChange(it.readBytes())
//            }
//        }
//    }
    SettingsTileScaffold(
        title = title,
        enabled = enabled,
        subtitle = subTitle,
        modifier = modifier.clickable(enabled) {
            scope.launch {
                val path = FilePicker.openFilePicker(
                    ext = extensions
                )
                if (path != null) {
                    onValueChange(path.buffer().use(BufferedSource::readByteArray))
                }
            }
        },
    )
}
