package top.kagg886.pmf.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold

@Composable
fun SettingsTextField(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},

    dialogTitle: @Composable () -> Unit = { Text("编辑属性") },
    dialogLabel: @Composable () -> Unit = {},
    dialogPlaceHolder: @Composable () -> Unit = { Text("请输入") },

    value: String,
    onValueChange: (String) -> Unit,
) {
    var expand by remember {
        mutableStateOf(false)
    }

    if (expand) {
        var v by remember(value) {
            mutableStateOf(value)
        }
        AlertDialog(
            onDismissRequest = { expand = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        expand = false
                        onValueChange(v)
                    }
                ) {
                    Text("确定")
                }
            },
            title = dialogTitle,
            text = {
                OutlinedTextField(
                    value = v,
                    onValueChange = {
                        v = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = dialogLabel,
                    placeholder = dialogPlaceHolder,
                )
            },
        )
    }

    SettingsTileScaffold(
        title = title,
        subtitle = subTitle,
        modifier = modifier.clickable {
            expand = true
        },
    )
}