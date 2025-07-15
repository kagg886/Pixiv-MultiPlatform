package top.kagg886.pmf.ui.route.crash

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.core.FileKit
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.Res
import top.kagg886.pmf.app_crash
import top.kagg886.pmf.app_crash_message
import top.kagg886.pmf.app_crash_title
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.confirm
import top.kagg886.pmf.ui.component.icon.Github
import top.kagg886.pmf.ui.component.icon.Save

private fun getHostEnvironment(): String = buildString {
    appendLine("App Version: ${BuildConfig.APP_VERSION_NAME}(${BuildConfig.APP_VERSION_CODE}) --- ${BuildConfig.APP_COMMIT_ID}")
    appendLine("Running Platform: ${currentPlatform.name}")
    // Smart cast to 'Platform.Android' is impossible, because 'currentPlatform' is a expect property.
    (currentPlatform as? Platform.Android)?.let {
        appendLine("    Version: ${it.version}")
    }
    appendLine("App Locale: ${Locale.current}")
}

@Composable
fun CrashApp(
    modifier: Modifier = Modifier,
    throwable: String,
    onExitHandler: () -> Unit = { exitProcess(0) },
) {
    var dialog by remember {
        mutableStateOf(true)
    }
    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = {
                Text(stringResource(Res.string.app_crash_title))
            },
            text = {
                Text(stringResource(Res.string.app_crash_message))
            },
            confirmButton = {
                Button(onClick = { dialog = false }) {
                    Text(stringResource(Res.string.confirm))
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
                    Text(stringResource(Res.string.app_crash))
                },
                navigationIcon = {
                    if (currentPlatform !is Platform.Desktop) {
                        IconButton(onClick = onExitHandler) {
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
                                    buildString {
                                        appendLine(getHostEnvironment())
                                        appendLine(throwable)
                                    }
                                },
                            )
                            handler.openUri("https://github.com/kagg886/Pixiv-MultiPlatform/issues/new/choose")
                        }) {
                            Icon(imageVector = Github, contentDescription = null)
                        }
                        val scope = rememberCoroutineScope()
                        IconButton(
                            onClick = {
                                scope.launch {
                                    FileKit.saveFile(
                                        bytes = buildString {
                                            appendLine(getHostEnvironment())
                                            appendLine(throwable)
                                        }.encodeToByteArray(),
                                        baseName = "${BuildConfig.APP_NAME} Crash Info - ${Clock.System.now()}",
                                        extension = "log",
                                    )
                                }
                            },
                        ) {
                            Icon(imageVector = Save, contentDescription = null)
                        }
                    }
                },
            )
        },
    ) {
        Text(
            text = buildString {
                appendLine(getHostEnvironment())
                appendLine(throwable.replace("\t", "    "))
            },
            modifier = Modifier.padding(it).padding(horizontal = 5.dp)
                .verticalScroll(rememberScrollState()),
        )
    }
}

expect fun exitProcess(i: Int): Nothing
