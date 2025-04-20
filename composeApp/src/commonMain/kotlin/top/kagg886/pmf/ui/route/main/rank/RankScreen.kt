package top.kagg886.pmf.ui.route.main.rank

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pmf.Res
import top.kagg886.pmf.allStringResources
import top.kagg886.pmf.ui.component.TabContainer

@Composable
fun Screen.RankScreen() {
    val page = rememberScreenModel {
        object : ScreenModel {
            val page = mutableIntStateOf(0)
        }
    }
    val index = page.page.collectAsState()
    TabContainer(
        modifier = Modifier.fillMaxSize(),
        tab = RankCategory.entries,
        tabTitle = { Text(stringResource(Res.allStringResources["rank_${it.content}"]!!)) },
        current = index,
        scrollable = true,
        onCurrentChange = { page.page.tryEmit(it) },
    ) {
        IllustContent(rank = it)
    }
}
