import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import top.kagg886.pmf.ui.component.collapsable.v3.CollapsableTopAppBarScaffold

class CollapseV2Screen:Screen {
    @Composable
    override fun Content() {
        CollapsableTopAppBarScaffold(
            modifier = Modifier.fillMaxSize(),
            background = {
                Box(
                    modifier = it.fillMaxWidth().height(200.dp).background(Color.Red)
                ) {
                    Text("Hello World")
                }
            },
            title = {
                Text("QWQ")
            },
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack,null)
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Menu,null)
                }
            },
        ) {
            val state = rememberLazyListState()
            LazyColumn(
                it.fixComposeListScrollToTopBug(state),
                state = state
            ) {
                items(100) {
                    ListItem(
                        headlineContent = {
                            Text("Hello World $it")
                        }
                    )
                }
            }
        }
    }
}