package top.kagg886.pmf.ui.route.main.download

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.pixko.module.illust.get
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.database.dao.DownloadItemType
import top.kagg886.pmf.backend.database.dao.illust
import top.kagg886.pmf.backend.database.dao.novel
import top.kagg886.pmf.download_success
import top.kagg886.pmf.page_is_empty
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.icon.Download
import top.kagg886.pmf.ui.component.icon.Save
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen
import top.kagg886.pmf.util.stringResource

class DownloadScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<DownloadScreenModel>()
        val state by model.collectAsState()
        DownloadContent(
            model,
            state,
        )
    }

    @Composable
    private fun DownloadContent(model: DownloadScreenModel, state: DownloadScreenState) {
        when (state) {
            DownloadScreenState.Loading -> {
                Loading()
            }

            is DownloadScreenState.Loaded -> {
                val data by state.data.collectAsState(emptyList())
                if (data.isEmpty()) {
                    ErrorPage(text = stringResource(Res.string.page_is_empty)) {
                    }
                    return
                }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                    items(data) { item ->
                        when (item.meta) {
                            DownloadItemType.ILLUST -> {
                                IllustDownloadItem(
                                    item = item,
                                    model = model,
                                    modifier = Modifier.padding(5.dp),
                                )
                            }
                            DownloadItemType.NOVEL -> {
                                NovelDownloadItem(
                                    item = item,
                                    model = model,
                                    modifier = Modifier.padding(5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun IllustDownloadItem(
        item: top.kagg886.pmf.backend.database.dao.DownloadItem,
        model: DownloadScreenModel,
        modifier: Modifier = Modifier,
    ) {
        val nav = LocalNavigator.currentOrThrow
        OutlinedCard(
            modifier = modifier,
            onClick = { nav.push(IllustDetailScreen(item.illust)) },
        ) {
            Row(
                modifier = Modifier.padding(5.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = item.illust.contentImages.get()!![0],
                    modifier = Modifier.size(75.dp, 120.dp).clip(CardDefaults.shape),
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                )
                ListItem(
                    overlineContent = {
                        Text(item.illust.id.toString())
                    },
                    headlineContent = {
                        Text(item.illust.title, maxLines = 1)
                    },
                    trailingContent = {
                        when {
                            item.progress == -1f && !item.success -> {
                                IconButton(
                                    onClick = {
                                        model.startIllustDownload(item.illust)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Download,
                                        contentDescription = null,
                                    )
                                }
                            }

                            item.progress == -1f && item.success -> {
                                Row {
                                    IconButton(
                                        onClick = {
                                            model.startIllustDownloadOr(item) {
                                                model.saveToExternalFile(item)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Save,
                                            contentDescription = null,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            model.startIllustDownloadOr(item) {
                                                model.shareFile(item)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }

                            else -> CircularProgressIndicator()
                        }
                    },
                    supportingContent = {
                        if (item.success) {
                            Text(stringResource(Res.string.download_success))
                            return@ListItem
                        }
                        if (item.progress != -1f) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                progress = { item.progress },
                            )
                        }
                    },
                )
            }
        }
    }

    @Composable
    private fun NovelDownloadItem(
        item: top.kagg886.pmf.backend.database.dao.DownloadItem,
        model: DownloadScreenModel,
        modifier: Modifier = Modifier,
    ) {
        val nav = LocalNavigator.currentOrThrow
        OutlinedCard(
            modifier = modifier,
            onClick = { nav.push(NovelDetailScreen(item.novel.id.toLong())) },
        ) {
            Row(
                modifier = Modifier.padding(5.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = item.novel.imageUrls.content,
                    modifier = Modifier.size(75.dp, 120.dp).clip(CardDefaults.shape),
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                )
                ListItem(
                    overlineContent = {
                        Text(item.novel.id.toString())
                    },
                    headlineContent = {
                        Text(item.novel.title, maxLines = 1)
                    },
                    trailingContent = {
                        when {
                            item.progress == -1f && !item.success -> {
                                IconButton(
                                    onClick = {
                                        model.startNovelDownload(item.novel)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Download,
                                        contentDescription = null,
                                    )
                                }
                            }

                            item.progress == -1f && item.success -> {
                                Row {
                                    IconButton(
                                        onClick = {
                                            model.startNovelDownloadOr(item) {
                                                model.saveToExternalFile(item)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Save,
                                            contentDescription = null,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            model.startNovelDownloadOr(item) {
                                                model.shareFile(item)
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }

                            else -> CircularProgressIndicator()
                        }
                    },
                    supportingContent = {
                        if (item.success) {
                            Text(stringResource(Res.string.download_success))
                            return@ListItem
                        }
                        if (item.progress != -1f) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                progress = { item.progress },
                            )
                        }
                    },
                )
            }
        }
    }
}
