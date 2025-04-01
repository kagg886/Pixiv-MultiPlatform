package top.kagg886.pmf.ui.route.crash

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.ui.component.icon.Github

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashApp(modifier: Modifier = Modifier, throwable: String) {
    var dialog by remember {
        mutableStateOf(true)
    }
    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = {
                Text("应用崩溃了！")
            },
            text = {
                Text(
                    buildAnnotatedString {
                        appendLine("很遗憾的通知您：Pixiv-MultiPlatform 在运行时出现了无法恢复的错误，因此应用程序即将关闭。")
                        appendLine("为了帮助我们修复这个bug以避免这种情况的再次发生，请关闭对话框并点击右上角的按钮，以提交本次错误报告。")
                    },
                )
            },
            confirmButton = {
                Button(onClick = { dialog = false }) {
                    Text("好")
                }
            },
        )
    }
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                dialog = true
            }) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("崩溃报告！")
                },
                navigationIcon = {
                    if (currentPlatform !is Platform.Desktop) {
                        IconButton(onClick = { exitProcess(0) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                actions = {
                    Row {
                        val clip = LocalClipboardManager.current
                        val handler = LocalUriHandler.current
                        IconButton(onClick = {
                            clip.setText(
                                buildAnnotatedString {
                                    append(throwable)
                                },
                            )
                            handler.openUri("https://github.com/kagg886/Pixiv-MultiPlatform/issues/new/choose")
                        }) {
                            Icon(imageVector = Github, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) {
        Text(
            text = throwable.replace("\t", "    "),
            modifier = Modifier.padding(it).padding(horizontal = 5.dp).verticalScroll(rememberScrollState()),
        )
    }
}

expect fun exitProcess(i: Int): Nothing
