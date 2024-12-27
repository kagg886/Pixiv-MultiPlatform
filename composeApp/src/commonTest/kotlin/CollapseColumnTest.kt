import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.junit.Test
import top.kagg886.pmf.ui.component.collapsable.v2.CollapsableTopAppBarScaffold

class CollapseColumnTest {

    @Test
    fun testUI() {
        application {
            Window(onCloseRequest = ::exitApplication) {
                Content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content() {
    CollapsableTopAppBarScaffold(
        toolbarSize = TopAppBarDefaults.LargeAppBarExpandedHeight,
        toolbar = {
            LargeTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(text = "qwq")
                }
            )
        },
        smallToolBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(text = "qwq")
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.nestedScroll(it)) {
            items(100) {
                ListItem(
                    headlineContent = {
                        Text(it.toString())
                    }
                )
            }
        }
    }
}