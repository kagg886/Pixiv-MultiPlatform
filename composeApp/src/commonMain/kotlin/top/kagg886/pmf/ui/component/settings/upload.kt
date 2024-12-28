package top.kagg886.pmf.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

@Composable
fun SettingsFileUpload(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},
    onValueChange: (ByteArray) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val launcher = rememberFilePickerLauncher {
        if (it != null) {
            scope.launch {
                onValueChange(it.readBytes())
            }
        }
    }
    SettingsTileScaffold(
        title = title,
        enabled = enabled,
        subtitle = subTitle,
        modifier = modifier.clickable(enabled) {
            launcher.launch()
        },
    )
}