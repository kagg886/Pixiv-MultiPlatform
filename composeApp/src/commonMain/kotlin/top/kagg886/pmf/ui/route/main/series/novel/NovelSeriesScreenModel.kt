package top.kagg886.pmf.ui.route.main.series.novel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.module.novel.SeriesDetail
import top.kagg886.pixko.module.novel.SeriesInfo
import top.kagg886.pixko.module.novel.getNovelSeries
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.container

class NovelSeriesScreenModel(
    private val seriesId: Int
) : ViewModel(), ScreenModel, KoinComponent,
    ContainerHost<NovelSeriesScreenState, NovelSeriesScreenSideEffect> {
    private val client = PixivConfig.newAccountFromConfig()
    override val container: Container<NovelSeriesScreenState, NovelSeriesScreenSideEffect> =
        container(NovelSeriesScreenState.Loading) {
            reload()
        }

    fun reload() = intent {
        reduce {
            NovelSeriesScreenState.Loading
        }

        val data = kotlin.runCatching {
            client.getNovelSeries(seriesId)
        }
        if (data.isFailure) {
            reduce {
                NovelSeriesScreenState.LoadingFailed(data.exceptionOrNull()!!.message ?: "未知错误")
            }
            return@intent
        }

        reduce {
            NovelSeriesScreenState.LoadingSuccess(data.getOrThrow().novelSeriesDetail)
        }
    }

}

sealed interface NovelSeriesScreenState {
    data object Loading : NovelSeriesScreenState
    data class LoadingSuccess(val info: SeriesDetail) : NovelSeriesScreenState
    data class LoadingFailed(val msg: String) : NovelSeriesScreenState
}

interface NovelSeriesScreenSideEffect {

}