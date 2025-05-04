package top.kagg886.pmf.ui.route.main.series.novel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.novel.SeriesDetail
import top.kagg886.pixko.module.novel.getNovelSeries
import top.kagg886.pixko.module.user.UserLikePublicity
import top.kagg886.pixko.module.user.followUser
import top.kagg886.pixko.module.user.unFollowUser
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.follow_fail
import top.kagg886.pmf.follow_success
import top.kagg886.pmf.follow_success_private
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.unfollow_fail
import top.kagg886.pmf.unfollow_success
import top.kagg886.pmf.unknown_error

class NovelSeriesScreenModel(
    private val seriesId: Int,
) : ViewModel(),
    ScreenModel,
    KoinComponent,
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
            val unknown = getString(Res.string.unknown_error)
            reduce {
                NovelSeriesScreenState.LoadingFailed(data.exceptionOrNull()!!.message ?: unknown)
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
                client.followUser(state.info.user.id, if (private) UserLikePublicity.PRIVATE else UserLikePublicity.PUBLIC)
            }
            if (result.isFailure) {
                postSideEffect(NovelSeriesScreenSideEffect.Toast(getString(Res.string.follow_fail)))
                return@runOn
            }
            if (private) {
                postSideEffect(NovelSeriesScreenSideEffect.Toast(getString(Res.string.follow_success_private)))
            } else {
                postSideEffect(NovelSeriesScreenSideEffect.Toast(getString(Res.string.follow_success)))
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
                postSideEffect(NovelSeriesScreenSideEffect.Toast(getString(Res.string.unfollow_fail)))
                return@runOn
            }
            postSideEffect(NovelSeriesScreenSideEffect.Toast(getString(Res.string.unfollow_success)))
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
