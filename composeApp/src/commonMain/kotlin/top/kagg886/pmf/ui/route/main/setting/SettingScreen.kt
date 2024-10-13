package top.kagg886.pmf.ui.route.main.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.java.KoinJavaComponent.getKoin
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.ui.route.main.about.AboutScreen
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.b
import top.kagg886.pmf.ui.util.mb


class SettingScreen :Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        Column {
            if (currentPlatform.useWideScreenMode) {
                TopAppBar(
                    title = {
                        Text("设置")
                    },
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text("所有选项均重启才能生效。")
                }
                item {
                    Column {
                        var value by remember {
                            mutableStateOf(AppConfig.defaultGalleryWidth.toFloat())
                        }
                        ListItem(
                            headlineContent = {
                                Text("画廊列数,当前值:$value")
                            },
                            supportingContent = {
                                LaunchedEffect(value) {
                                    AppConfig.defaultGalleryWidth = value.toInt()
                                }
                                Slider(
                                    value = value,
                                    onValueChange = {
                                        value = it
                                    },
                                    valueRange = 2f..5f,
                                    steps = 2
                                )
                            }
                        )

                    }
                }

                item {
                    var value by remember {
                        mutableStateOf(AppConfig.cacheSize.toFloat())
                    }
                    ListItem(
                        headlineContent = {
                            Text("画廊本地缓存大小,当前值:${value.b}")
                        },
                        supportingContent = {
                            LaunchedEffect(value) {
                                AppConfig.cacheSize = value.toLong()
                            }
                            Slider(
                                value = value,
                                onValueChange = {
                                    value = it
                                },
                                valueRange = 10.mb.bytes.toFloat()..2048.mb.bytes.toFloat(),
                            )
                        }
                    )
                }

                item {
                    var value by remember {
                        mutableStateOf(AppConfig.byPassSNI)
                    }
                    ListItem(
                        headlineContent = {
                            Text("SNI Bypass")
                        },
                        supportingContent = {
                            Text("绕过Pixiv SNI Checker，可实现免VPN直连。若拥有梯子环境请关闭此功能以提高加载速度")
                        },
                        trailingContent = {
                            LaunchedEffect(value) {
                                AppConfig.byPassSNI = value
                            }
                            Switch(
                                checked = value,
                                onCheckedChange = {
                                    value = it
                                }
                            )
                        }
                    )
                }
                item {
                    val model = remember {
                        getKoin().get<UpdateCheckViewModel>()
                    }
                    ListItem(
                        headlineContent = {
                            Text("检查更新")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Build, "")
                        },
                        modifier = Modifier.clickable {
                            model.checkUpdate()
                        }
                    )
                }
                item {
                    val nav = LocalNavigator.currentOrThrow
                    ListItem(
                        headlineContent = {
                            Text("关于")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Info, "")
                        },
                        modifier = Modifier.clickable {
                            nav.push(AboutScreen())
                        }
                    )
                }
            }

        }
    }

}
