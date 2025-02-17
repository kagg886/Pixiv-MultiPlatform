package top.kagg886.pmf.ui.route.main.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.rememberLibraries
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.kotlin
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.collapsable.v3.CollapsableTopAppBarScaffold
import top.kagg886.pmf.ui.component.icon.Github
import top.kagg886.pmf.ui.component.icon.Update

class AboutScreen : Screen {
    @OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val libraries by rememberLibraries {
            Res.readBytes("files/aboutlibraries.json").decodeToString()
        }

        val lib = remember(libraries) {
            libraries ?: Libs(persistentListOf(), persistentSetOf())
        }

        CollapsableTopAppBarScaffold(
            modifier = Modifier.fillMaxSize(),
            background = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedCard(
                        it.fillMaxWidth(0.9f)
                            .padding(top = TopAppBarDefaults.MediumAppBarCollapsedHeight + 16.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Spacer(Modifier.height(16.dp))
                        Image(
                            painter = painterResource(Res.drawable.kotlin),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = BuildConfig.APP_NAME,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${BuildConfig.APP_VERSION_NAME} | ${BuildConfig.APP_VERSION_CODE} (Code by kagg886)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ListItemDefaults.colors().supportingTextColor,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.align(Alignment.CenterHorizontally)) {
                            val uri = LocalUriHandler.current
                            IconButton(
                                onClick = {
                                    uri.openUri("https://github.com/kagg886/Pixiv-MultiPlatform")
                                }
                            ) {
                                Icon(imageVector = Github, contentDescription = null)
                            }
                            IconButton(
                                onClick = {
                                    uri.openUri("https://pmf.kagg886.top")
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Home, contentDescription = null)
                            }
                            IconButton(
                                onClick = {
                                    uri.openUri("https://github.com/kagg886/Pixiv-MultiPlatform/releases")
                                }
                            ) {
                                Icon(imageVector = Update, contentDescription = null)
                            }
                        }
                    }
                }
            },
            title = {
                Text("开源许可")
            },
            navigationIcon = {
                val nav = LocalNavigator.currentOrThrow
                IconButton(
                    onClick = { nav.pop() }
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        ) {
            val state = rememberLazyListState()
            LazyColumn(it.fillMaxWidth().fixComposeListScrollToTopBug(state), state = state) {
                items(lib.libraries) {
                    val uri = LocalUriHandler.current
                    OutlinedCard(
                        Modifier.fillMaxWidth().padding(8.dp).clickable {
                            it.website?.let { u -> uri.openUri(u) }
                        }
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(it.name)
                            },
                            supportingContent = {
                                Text(
                                    text = it.description?.ifBlank { "No Descriptions." } ?: "No Descriptions.",
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                it.artifactVersion?.let { u -> Text(u) }
                            }
                        )
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                        ) {
                            for (i in it.licenses) {
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(i.name)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
//        Scaffold(
//            topBar = {
//                val nav = LocalNavigator.currentOrThrow
//                TopAppBar(
//                    title = { Text("关于") },
//                    navigationIcon = {
//                        IconButton(onClick = { nav.pop() }) {
//                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
//                        }
//                    }
//                )
//            }
//        ) {
//
//            val snack = LocalSnackBarHost.current
//            val scope = rememberCoroutineScope()
//            LibrariesContainer(
//                libraries = libraries,
//                modifier = Modifier.fillMaxSize().padding(it).padding(horizontal = 32.dp),
//                onLibraryClick = {
//                    scope.launch {
//                        if (it.website != null) {
//                            uri.openUri(it.website!!)
//                        } else {
//                            snack.showSnackbar("暂无官网链接")
//                        }
//                    }
//                },
//                header = {
//                    item {
//                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
//                            OutlinedCard(modifier = Modifier.fillMaxWidth(0.8f).fillMaxSize()) {
//                                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
//                                    Spacer(modifier = Modifier.height(16.dp))
//                                    Image(
//                                        painter = painterResource(Res.drawable.kotlin),
//                                        contentDescription = null,
//                                        modifier = Modifier.size(64.dp)
//                                    )
//                                    Spacer(modifier = Modifier.height(16.dp))
//                                    Text(BuildConfig.APP_NAME, style = MaterialTheme.typography.titleLarge)
//                                    Spacer(modifier = Modifier.height(16.dp))
//                                    Text(
//                                        "${BuildConfig.APP_VERSION_NAME} | ${BuildConfig.APP_VERSION_CODE} (Code by kagg886)",
//                                        color = Color.Gray
//                                    )
//                                    Spacer(modifier = Modifier.height(16.dp))
//                                    Row(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        horizontalArrangement = Arrangement.SpaceAround,
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {

//                                    }
//                                    Spacer(modifier = Modifier.height(16.dp))
//                                }
//                            }
//                        }
//                    }
//                }
//            )
//        }


    }
}
