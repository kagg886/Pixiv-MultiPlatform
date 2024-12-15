package top.kagg886.pmf.ui.route.crash

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashApp(modifier: Modifier = Modifier, throwable: Throwable) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("应用意外崩溃！")
                },
                navigationIcon = {
                    IconButton(onClick = { exitProcess(0) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    }
                },
                actions = {
                    Row {
                        val clip = LocalClipboardManager.current
                        IconButton(onClick = {
                            clip.setText(
                                buildAnnotatedString {
                                    append(throwable.stackTraceToString())
                                }
                            )
                        }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) {

        Text(text = buildString {
            appendLine("请单击右上角按钮复制堆栈后，反馈给仓库issue区")
            appendLine(throwable.stackTraceToString())
        }, modifier = Modifier.verticalScroll(rememberScrollState()).padding(it))
    }
}