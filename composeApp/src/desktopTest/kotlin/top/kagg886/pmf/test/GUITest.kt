package top.kagg886.pmf.test

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.fetch.newBase64Uri
import okio.Path.Companion.toPath
import okio.buffer
import org.junit.Test
import org.koin.core.component.KoinComponent
import top.kagg886.pmf.launchApp
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.util.absolutePath
import top.kagg886.pmf.util.source

class GUITest {
    @Test
    fun testGUI1() {
        launchApp(init = {GIFScreen()})
    }

    @Test
    fun testGUI2() {
        launchApp(init = {IllustDetailScreen.PreFetch(127775686)})
    }



    class GIFScreen : Screen, KoinComponent {
        @Composable
        override fun Content() {
            AsyncImage(
                uri = newBase64Uri("image/gif","C:\\Users\\kagg886\\IdeaProjects\\Pixiv-MultiPlatform\\lib\\gif\\output.gif".toPath().absolutePath().source().buffer().readByteArray()),
                contentDescription = ""
            )
        }
    }
}
