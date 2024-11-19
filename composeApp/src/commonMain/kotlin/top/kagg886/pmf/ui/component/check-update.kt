package top.kagg886.pmf.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import org.koin.java.KoinJavaComponent.getKoin
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.ui.util.*
import java.io.File

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
                        Text("更新：${BuildConfig.APP_VERSION_NAME} --> ${state.release.versionName}")
                    },
                    text = {
                        Text(state.release.body)
                    },
                    confirmButton = {
                        val uri = state.release.assets.first {
                            File(it.name).nameWithoutExtension == currentPlatform.name
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