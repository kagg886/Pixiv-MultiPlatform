package top.kagg886.pmf.test

import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import cafe.adriel.voyager.core.screen.Screen
import co.touchlab.kermit.Logger
import com.github.panpf.sketch.LocalPlatformContext
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.sketch
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.e404.skiko.gif.gif
import top.kagg886.gif.ImageBitmapDelegate
import top.kagg886.gif.toImageBitmap
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.ugoira.getUgoiraMetadata
import top.kagg886.pmf.backend.cachePath
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.launchApp
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.writeBytes

class GUITest {
    @Test
    fun testGUI1() {
        launchApp(init = GIFScreen())
    }


    class GIFScreen : Screen, KoinComponent {
        @Composable
        override fun Content() {
            val scope = rememberCoroutineScope()
            println("composed")
            Button(
                onClick = {
                    val account = PixivConfig.newAccountFromConfig()
                    val client by inject<HttpClient>()

                    scope.launch {
                        val illust = account.getIllustDetail(127775686)
                        val meta = account.getUgoiraMetadata(illust)
                        Logger.i("get the ugoira data")
                        val zip = FileSystem.SYSTEM.openZip(
                            cachePath.resolve("gif.zip")
                        )

                        Logger.i("open zip success")

                        val frames = meta.frames.map {
                            zip.source(it.file.toPath()).toImageBitmap() to it.delay
                        }

                        Logger.i("get the frames")

                        val data = gif(illust.width,illust.height) {
                            table(ImageBitmapDelegate(frames[0].first))
                            loop(0)

                            frame(ImageBitmapDelegate(frames[0].first))

                            for (i in 1 until frames.size) {
                                frame(ImageBitmapDelegate(frames[i].first)) {
                                    duration = frames[i].second
                                }
                            }
                        }

                        data.buildToSink("out.gif".toPath().sink().buffer())
                        Logger.i("write to out.png")
                    }
                }
            ) {
                Text("start GIF")
            }
        }
    }
}
