package top.kagg886.pmf.ui.component.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.koin.mp.KoinPlatform.getKoin
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.ui.util.*

@Composable
fun CheckUpdateDialog() {
    val updateModel by getKoin().inject<UpdateCheckViewModel>()
    val state by updateModel.collectAsState()
    val s = LocalSnackBarHost.current
    updateModel.collectSideEffect {
        when (it) {
            is UpdateCheckSideEffect.Toast -> {
                s.showSnackbar(it.msg)
            }
        }
    }

    UpdateCheckDialogContent(state)
}

@Composable
private fun UpdateCheckDialogContent(state: UpdateCheckState) {
    val updateModel by getKoin().inject<UpdateCheckViewModel>()
    when (state) {
        is UpdateCheckState.HaveUpdate -> {
            if (!state.dismiss) {
                AlertDialog(
                    onDismissRequest = {
                        updateModel.dismiss()
                    },
                    title = {
                        Text("更新：v${BuildConfig.APP_VERSION_NAME} --> ${state.release.tagName}")
                    },
                    text = {
                        Text(state.release.body)
                    },
                    confirmButton = {
                        val uri = state.release.assets.find {
                            it.name.startsWith(currentPlatform.name)
                        }
                        if (uri == null) {
                            Text("暂不支持该平台", modifier = Modifier.padding(ButtonDefaults.TextButtonContentPadding))
                            return@AlertDialog
                        }
                        val handler = LocalUriHandler.current
                        TextButton(
                            onClick = {
                                handler.openUri(uri.download)
                            }
                        ) {
                            Text("下载")
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(
                                onClick = {
                                    updateModel.dismiss()
                                }
                            ) {
                                Text("当前版本不再提示")
                            }
                        }
                    }
                )
            }
        }

        UpdateCheckState.Loading -> {}
    }
}
