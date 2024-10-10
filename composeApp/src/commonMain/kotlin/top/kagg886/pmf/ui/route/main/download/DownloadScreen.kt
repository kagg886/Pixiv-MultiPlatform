package top.kagg886.pmf.ui.route.main.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import okhttp3.HttpUrl.Companion.toHttpUrl
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.rootPath
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.icon.Download
import top.kagg886.pmf.ui.util.collectAsState

class DownloadScreen(val isOpenInSideBar:Boolean = false) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = koinScreenModel<DownloadScreenModel>()

        val state by model.collectAsState()
        val nav = LocalNavigator.currentOrThrow
        Column {
            if (currentPlatform.useWideScreenMode) {
                TopAppBar(
                    title = { Text("下载列表") },
                    navigationIcon = {
                        if (!isOpenInSideBar) {
                            IconButton(onClick = {
                                nav.pop()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
            }
            DownloadContent(
                model, state
            )
        }
    }

    @Composable
    private fun DownloadContent(model: DownloadScreenModel, state: DownloadScreenState) {
        when (state) {
            DownloadScreenState.Loading -> {
                Loading()
            }

            is DownloadScreenState.Loaded -> {
                val data by state.illust.collectAsState(emptyList())
                if (data.isEmpty()) {
                    ErrorPage(text = "没有项目！") {

                    }
                    return
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(data) {
                        val progressState by model.downloadFlow(it.id).collectAsState(-1f)
                        ListItem(
                            headlineContent = {
                                Text(it.url)
                            },
                            trailingContent = {
                                when {
                                    progressState == -1f && !it.success -> {
                                        IconButton(
                                            onClick = {
                                                model.startDownload(it)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Download,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    progressState == -1f && it.success -> {
                                        IconButton(
                                            onClick = {
                                                shareFile(
                                                    rootPath.resolve("download").resolve(
                                                        it.url.toHttpUrl().encodedPathSegments.last()
                                                    )
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    else -> CircularProgressIndicator()
                                }
                            },
                            supportingContent = {
                                if (it.success) {
                                    Text("下载完成")
                                    return@ListItem
                                }
                                if (progressState != -1f) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(0.8f),
                                        progress = { progressState })
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}