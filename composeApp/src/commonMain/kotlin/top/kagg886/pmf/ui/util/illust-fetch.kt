package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.bookmarkIllust
import top.kagg886.pixko.module.illust.deleteBookmarkIllust
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import kotlin.coroutines.CoroutineContext

abstract class IllustFetchViewModel : ContainerHost<IllustFetchViewState, IllustFetchSideEffect>, ViewModel(),
    ScreenModel {
    private val scope = viewModelScope + Dispatchers.IO

    protected val client = PixivConfig.newAccountFromConfig()

    private var repo: InfinityRepository<Illust>? = null


    override val container: Container<IllustFetchViewState, IllustFetchSideEffect> =
        container(IllustFetchViewState.Loading) {
            initIllust()
        }

    private fun Sequence<Illust>.filterUserCustomSettings() = this
        .filter { !AppConfig.filterAi || it.isAI }
        .filter { !AppConfig.filterR18 || it.isR18 }
        .filter { !AppConfig.filterR18G || it.isR18G }

    abstract fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust>

    fun initIllust(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                IllustFetchViewState.Loading
            }
        }
        repo = initInfinityRepository(scope.coroutineContext)
        reduce {
            IllustFetchViewState.ShowIllustList(
                repo!!.filterUserCustomSettings().take(20).toList(),
                noMoreData = repo!!.noMoreData
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMoreIllusts() = intent {
        runOn<IllustFetchViewState.ShowIllustList> {
            reduce {
                state.copy(
                    illusts = state.illusts + repo!!.filterUserCustomSettings().take(20).toList(),
                    noMoreData = repo!!.noMoreData
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun likeIllust(illust: Illust) = intent {
        runOn<IllustFetchViewState.ShowIllustList> {
            val result = kotlin.runCatching {
                client.bookmarkIllust(illust.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustFetchSideEffect.Toast("收藏失败~"))
                return@runOn
            }
            postSideEffect(IllustFetchSideEffect.Toast("收藏成功~"))
            reduce {
                state.copy(
                    illusts = state.illusts.map {
                        if (it.id == illust.id) {
                            it.copy(isBookMarked = true)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeIllust(illust: Illust) = intent {
        runOn<IllustFetchViewState.ShowIllustList> {
            val result = kotlin.runCatching {
                client.deleteBookmarkIllust(illust.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustFetchSideEffect.Toast("取消收藏失败~"))
                return@runOn
            }
            postSideEffect(IllustFetchSideEffect.Toast("取消收藏成功~"))
            reduce {
                state.copy(
                    illusts = state.illusts.map {
                        if (it == illust) {
                            it.copy(isBookMarked = false)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }
}

sealed class IllustFetchViewState {
    data object Loading : IllustFetchViewState()
    data class ShowIllustList(
        val illusts: List<Illust>,
        val noMoreData: Boolean = false,
        val scrollerState: LazyStaggeredGridState = LazyStaggeredGridState()
    ) : IllustFetchViewState()
}

sealed class IllustFetchSideEffect {
    data class Toast(val msg: String) : IllustFetchSideEffect()
}