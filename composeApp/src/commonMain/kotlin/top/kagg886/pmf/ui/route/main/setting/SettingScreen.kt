package top.kagg886.pmf.ui.route.main.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.koin.java.KoinJavaComponent.getKoin
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.ui.route.main.about.AboutScreen
import top.kagg886.pmf.ui.util.UpdateCheckViewModel
import top.kagg886.pmf.ui.util.b
import top.kagg886.pmf.ui.util.mb
import kotlin.time.Duration.Companion.seconds


class SettingScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
    @Composable
    override fun Content() {

        val snack = LocalSnackBarHost.current
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (currentPlatform.useWideScreenMode) {
                TopAppBar(
                    title = {
                        Text("设置")
                    },
                )
            }
            SettingsGroup(
                title = { Text(text = "画廊设置") },
            ) {
                var defaultGalleryWidth by remember {
                    mutableStateOf(AppConfig.defaultGalleryWidth.toFloat())
                }
                LaunchedEffect(defaultGalleryWidth) {
                    AppConfig.defaultGalleryWidth = defaultGalleryWidth.toInt()
                }
                SettingsSlider(
                    title = {
                        Text("画廊列数")
                    },
                    subtitle = {
                        Text("控制画廊页面的列数,当前值:$defaultGalleryWidth")
                    },
                    value = defaultGalleryWidth,
                    valueRange = 2f..5f,
                    steps = 2,
                    onValueChange = {
                        defaultGalleryWidth = it
                    }
                )

                var cacheSize by remember {
                    mutableStateOf(AppConfig.cacheSize.toFloat())
                }
                LaunchedEffect(cacheSize) {
                    snapshotFlow { cacheSize }.debounce(1.seconds).collectLatest {
                        AppConfig.cacheSize = cacheSize.toLong()
                    }
                }
                SettingsSlider(
                    title = {
                        Text("画廊本地缓存大小(重启生效)")
                    },
                    subtitle = {
                        Column {
                            Text("控制画廊页面的本地缓存大小,单位为字节。")
                            Text("当前值:${cacheSize.b}")
                        }
                    },
                    value = cacheSize,
                    valueRange = 10.mb.bytes.toFloat()..2048.mb.bytes.toFloat(),
                    onValueChange = {
                        cacheSize = it
                    }
                )

            }
            SettingsGroup(
                title = { Text("小说设置") }
            ) {
                var autoTypo by remember {
                    mutableStateOf(AppConfig.autoTypo)
                }
                LaunchedEffect(autoTypo) {
                    AppConfig.autoTypo = autoTypo
                }
                SettingsSwitch(
                    state = autoTypo,
                    title = {
                        Text("自动排版")
                    },
                    subtitle = {
                        Text("首行缩进24sp，行高24sp")
                    },
                    onCheckedChange = {
                        autoTypo = it
                    }
                )
            }
            SettingsGroup(
                title = { Text(text = "历史记录") }
            ) {
                var recordIllustHistory by remember {
                    mutableStateOf(AppConfig.recordIllustHistory)
                }
                LaunchedEffect(recordIllustHistory) {
                    AppConfig.recordIllustHistory = recordIllustHistory
                }
                SettingsSwitch(
                    state = recordIllustHistory,
                    title = {
                        Text("记录插画浏览记录")
                    },
                    subtitle = {
                        Text("关闭后将不会记录插画历史")
                    },
                    onCheckedChange = {
                        recordIllustHistory = it
                    }
                )

                var recordNovelHistory by remember {
                    mutableStateOf(AppConfig.recordNovelHistory)
                }
                LaunchedEffect(recordNovelHistory) {
                    AppConfig.recordNovelHistory = recordNovelHistory
                }
                SettingsSwitch(
                    state = recordNovelHistory,
                    title = {
                        Text("记录小说浏览记录")
                    },
                    subtitle = {
                        Text("关闭后将不会记录小说历史")
                    },
                    onCheckedChange = {
                        recordNovelHistory = it
                    }
                )

                var recordSearchHistory by remember {
                    mutableStateOf(AppConfig.recordSearchHistory)
                }
                LaunchedEffect(recordSearchHistory) {
                    AppConfig.recordSearchHistory = recordSearchHistory
                }
                SettingsSwitch(
                    state = recordSearchHistory,
                    title = {
                        Text("记录搜索记录")
                    },
                    subtitle = {
                        Text("关闭后将不会记录搜索记录")
                    },
                    onCheckedChange = {
                        recordSearchHistory = it
                    }
                )
            }
            SettingsGroup(
                title = { Text(text = "网络设置") }
            ) {
                var byPassSni by remember {
                    mutableStateOf(AppConfig.byPassSNI)
                }
                LaunchedEffect(byPassSni) {
                    AppConfig.byPassSNI = byPassSni
                }
                SettingsSwitch(
                    state = byPassSni,
                    title = {
                        Text("SNI Bypass(重启生效)")
                    },
                    subtitle = {
                        Text("绕过Pixiv SNI Checker，可实现免VPN直连。若拥有梯子环境请关闭此功能以提高加载速度")
                    },
                    onCheckedChange = {
                        byPassSni = it
                    }
                )
            }
            val model = remember {
                getKoin().get<UpdateCheckViewModel>()
            }
            SettingsMenuLink(
                title = {
                    Text("检查更新")
                },
                onClick = {
                    model.checkUpdate()
                },
                icon = {
                    Icon(Icons.Default.Build, "")
                }
            )
            val nav = LocalNavigator.currentOrThrow
            SettingsMenuLink(
                title = {
                    Text("关于")
                },
                onClick = {
                    nav.push(AboutScreen())
                },
                icon = {
                    Icon(Icons.Default.Info, "")
                }
            )
        }
    }

}
