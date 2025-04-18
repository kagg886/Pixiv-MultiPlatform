package top.kagg886.pmf.ui.route.main.rank

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pmf.Res
import top.kagg886.pmf.allStringResources
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.IllustFetchScreen

class RankScreen : Screen {
    private class PageScreenModel : ScreenModel {
        val page: MutableStateFlow<RankCategory> = MutableStateFlow(RankCategory.DAY)

        @Composable
        fun getPageState(): State<RankCategory> = page.collectAsState()
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun Content() {
        val page = rememberScreenModel {
            PageScreenModel()
        }
        val index by page.getPageState()
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

    @Composable
    private fun IllustContent(rank: RankCategory) {
        val model = rememberScreenModel(tag = "rank_$rank") {
            IllustRankScreenModel(type = rank)
        }
        IllustFetchScreen(model)
    }
}
