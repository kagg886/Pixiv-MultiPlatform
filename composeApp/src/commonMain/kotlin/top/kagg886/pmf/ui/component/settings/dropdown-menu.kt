package top.kagg886.pmf.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold

@Composable
inline fun <reified T> SettingsDropdownMenu(
    modifier: Modifier = Modifier,
    noinline title: @Composable () -> Unit,
    noinline subTitle: @Composable () -> Unit = {},
    noinline optionsFormat: @Composable (T) -> String = { it.toString() },
    current: T,
    data: List<T>,
    crossinline onSelected: (T) -> Unit,
) {
    var expand by remember {
        mutableStateOf(false)
    }
    SettingsTileScaffold(
        title = title,
        subtitle = subTitle,
        modifier = modifier.clickable {
            expand = true
        },
    ) {
        Text(optionsFormat(current))

        DropdownMenu(
            expanded = expand,
            onDismissRequest = {
                expand = false
            },
        ) {
            for (i in data) {
                DropdownMenuItem(
                    text = {
                        Text(optionsFormat(i))
                    },
                    onClick = {
                        onSelected(i)
                        expand = false
                    },
                )
            }
        }
    }
}
