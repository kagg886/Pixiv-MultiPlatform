package top.kagg886.pmf.ui.route.main.series.novel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.novel.SeriesDetail
import top.kagg886.pixko.module.novel.getNovelSeries
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
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

    @OptIn(OrbitExperimental::class)
    fun followUser(private: Boolean = false) = intent {
        runOn<NovelSeriesScreenState.LoadingSuccess> {
            val result = kotlin.runCatching {
                client.followUser(state.info.user.id,if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC)
            }
            if (result.isFailure) {
                postSideEffect(NovelSeriesScreenSideEffect.Toast("关注失败~"))
                return@runOn
            }
            if (private) {
                postSideEffect(NovelSeriesScreenSideEffect.Toast("悄悄关注是不想让别人看到嘛⁄(⁄ ⁄•⁄ω⁄•⁄ ⁄)⁄"))
            } else {
                postSideEffect(NovelSeriesScreenSideEffect.Toast("关注成功~"))
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun unFollowUser() = intent {
        runOn<NovelSeriesScreenState.LoadingSuccess> {
            val result = kotlin.runCatching {
                client.unFollowUser(state.info.user.id)
            }
            if (result.isFailure) {
                postSideEffect(NovelSeriesScreenSideEffect.Toast("取关失败~(*^▽^*)"))
                return@runOn
            }
            postSideEffect(NovelSeriesScreenSideEffect.Toast("取关成功~o(╥﹏╥)o"))
        }
    }
}

sealed interface NovelSeriesScreenState {
    data object Loading : NovelSeriesScreenState
    data class LoadingSuccess(val info: SeriesDetail) : NovelSeriesScreenState
    data class LoadingFailed(val msg: String) : NovelSeriesScreenState
}

interface NovelSeriesScreenSideEffect {
    data class Toast(val msg: String) : NovelSeriesScreenSideEffect
}
