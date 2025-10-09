package top.kagg886.pmf.ui.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold
import top.kagg886.pmf.res.*
import top.kagg886.pmf.util.stringResource

@Composable
fun SettingsTextField(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},
    dialogTitle: @Composable () -> Unit = { Text(stringResource(Res.string.edit_prop)) },
    dialogLabel: @Composable () -> Unit = {},
    dialogPlaceHolder: @Composable () -> Unit = { Text(stringResource(Res.string.please_input)) },
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
                    },
                ) {
                    Text(stringResource(Res.string.confirm))
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
