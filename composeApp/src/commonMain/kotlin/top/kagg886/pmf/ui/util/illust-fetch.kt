package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.plus
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.BookmarkVisibility
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.bookmarkIllust
import top.kagg886.pixko.module.illust.deleteBookmarkIllust
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.bookmark_failed
import top.kagg886.pmf.bookmark_success
import top.kagg886.pmf.un_bookmark_failed
import top.kagg886.pmf.un_bookmark_success

abstract class IllustFetchViewModel : ContainerHost<IllustFetchViewState, IllustFetchSideEffect>, ViewModel(), ScreenModel {

    protected val client = PixivConfig.newAccountFromConfig()

    private lateinit var repo: InfinityRepository<Illust>

    override val container: Container<IllustFetchViewState, IllustFetchSideEffect> =
        container(IllustFetchViewState.Loading) {
            initIllust()
        }

    private fun Flow<Illust>.filterUserCustomSettings() = this
        .filter { !it.isLimited }
        .filterNot { AppConfig.filterAi && it.isAI }
        .filterNot { AppConfig.filterR18G && it.isR18G }
        .filterNot { AppConfig.filterR18 && it.isR18 }

    abstract fun initInfinityRepository(): InfinityRepository<Illust>

    fun initIllust(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                IllustFetchViewState.Loading
            }
        }
        repo = initInfinityRepository()
        val list = repo.filterUserCustomSettings().take(20).toList()
        reduce { IllustFetchViewState.ShowIllustList(list, noMoreData = repo.noMoreData) }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMoreIllusts() = intent {
        runOn<IllustFetchViewState.ShowIllustList> {
            val list = state.illusts + repo.filterUserCustomSettings().take(20).toList()
            reduce { state.copy(illusts = list, noMoreData = repo.noMoreData) }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun likeIllust(
        illust: Illust,
        visibility: BookmarkVisibility = BookmarkVisibility.PUBLIC,
        tags: List<Tag>? = null,
    ) = intent {
        runOn<IllustFetchViewState.ShowIllustList> {
            val result = kotlin.runCatching {
                client.bookmarkIllust(illust.id.toLong()) {
                    this.visibility = visibility
                    this.tags = tags
                }
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustFetchSideEffect.Toast(getString(Res.string.bookmark_failed)))
                return@runOn
            }
            postSideEffect(IllustFetchSideEffect.Toast(getString(Res.string.bookmark_success)))
            reduce {
                state.copy(
                    illusts = state.illusts.map {
                        if (it.id == illust.id) {
                            it.copy(isBookMarked = true)
                        } else {
                            it
                        }
                    },
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
                postSideEffect(IllustFetchSideEffect.Toast(getString(Res.string.un_bookmark_failed)))
                return@runOn
            }
            postSideEffect(IllustFetchSideEffect.Toast(getString(Res.string.un_bookmark_success)))
            reduce {
                state.copy(
                    illusts = state.illusts.map {
                        if (it == illust) {
                            it.copy(isBookMarked = false)
                        } else {
                            it
                        }
                    },
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
        val scrollerState: LazyStaggeredGridState = LazyStaggeredGridState(),
    ) : IllustFetchViewState()
}

sealed class IllustFetchSideEffect {
    data class Toast(val msg: String) : IllustFetchSideEffect()
}
