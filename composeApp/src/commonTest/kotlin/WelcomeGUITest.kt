import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.junit.Test
import top.kagg886.pmf.ui.component.guide.GuideScaffold

class WelcomeGUITest {
    @Test
    fun testWelcomeGUI() = application {
        Window(onCloseRequest = ::exitApplication) {
            Column {
                GuideScaffold(
                    title = {
                        Text("欢迎使用")
                    },
                    subTitle = {
                        Text("第 -6步")
                    },
                    backButton = {
                        OutlinedButton(
                            onClick = {}
                        ) {
                            Text("上一步")
                        }
                    },
                    skipButton = {
                        TextButton(
                            onClick = {}
                        ) {
                            Text("跳过")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {}
                        ) {
                            Text("下一步")
                        }
                    }
                ) {
                    LazyColumn {
                        items(16) {
                            ListItem(
                                headlineContent = { Text("Items $it") }
                            )
                        }
                    }
                }
            }
        }
    }
}