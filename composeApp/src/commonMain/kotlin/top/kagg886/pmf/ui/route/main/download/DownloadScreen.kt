package top.kagg886.pmf.ui.route.main.download

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.illust.get
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.shareFile
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.component.icon.Download
import top.kagg886.pmf.ui.component.icon.Save
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.util.zip

class DownloadScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<DownloadScreenModel>()
        val state by model.collectAsState()
        DownloadContent(
            model, state
        )
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
                LazyColumn(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                    items(data) {
                        val nav = LocalNavigator.currentOrThrow
                        OutlinedCard(Modifier.padding(5.dp).clickable {
                            nav.push(IllustDetailScreen(it.illust))
                        }) {
                            Row(
                                modifier = Modifier.padding(5.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProgressedAsyncImage(
                                    url = it.illust.contentImages.get()!![0],
                                    modifier = Modifier.size(75.dp, 120.dp),
                                    contentScale = ContentScale.Inside
                                )
                                ListItem(
                                    overlineContent = {
                                        Text(it.illust.id.toString())
                                    },
                                    headlineContent = {
                                        Text(it.illust.title, maxLines = 1)
                                    },
                                    trailingContent = {
                                        when {
                                            it.progress == -1f && !it.success -> {
                                                IconButton(
                                                    onClick = {
                                                        model.startDownload(it.illust)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Download,
                                                        contentDescription = null
                                                    )
                                                }
                                            }

                                            it.progress == -1f && it.success -> {
                                                Row {
                                                    val scope = rememberCoroutineScope()
                                                    IconButton(
                                                        onClick = {
                                                            if (!it.downloadRootPath().exists()) {
                                                                model.startDownload(it.illust)
                                                                return@IconButton
                                                            }
                                                            scope.launch {
                                                                val listFiles = it.downloadRootPath().listFiles()!!
                                                                if (listFiles.size == 1) {
                                                                    FileKit.saveFile(
                                                                        bytes = listFiles[0].readBytes(),
                                                                        baseName = it.illust.title,
                                                                        extension = "png"
                                                                    )
                                                                    return@launch
                                                                }
                                                                FileKit.saveFile(
                                                                    bytes = it.downloadRootPath().zip(
                                                                        target = cachePath.resolve("share")
                                                                            .resolve("${it.id}.zip")
                                                                    ).readBytes(),
                                                                    baseName = "${it.illust.title}(${it.id})",
                                                                    extension = "zip"
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Save,
                                                            contentDescription = null
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (!it.downloadRootPath().exists()) {
                                                                model.startDownload(it.illust)
                                                                return@IconButton
                                                            }
                                                            val listFiles = it.downloadRootPath().listFiles()!!
                                                            if (listFiles.size == 1) {
                                                                shareFile(
                                                                    listFiles[0]
                                                                )
                                                                return@IconButton
                                                            }
                                                            shareFile(
                                                                it.downloadRootPath().zip(
                                                                    target = cachePath.resolve("share")
                                                                        .resolve("${it.id}.zip")
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
                                            }

                                            else -> CircularProgressIndicator()
                                        }
                                    },
                                    supportingContent = {
                                        if (it.success) {
                                            Text("下载完成")
                                            return@ListItem
                                        }
                                        if (it.progress != -1f) {
                                            LinearProgressIndicator(
                                                modifier = Modifier.fillMaxWidth(0.8f),
                                                progress = { it.progress })
                                        }
                                    }
                                )
                            }

                        }

                    }
                }
            }
        }
    }

}