package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.bookmarkNovel
import top.kagg886.pixko.module.novel.deleteBookmarkNovel
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import kotlin.coroutines.CoroutineContext

abstract class NovelFetchViewModel : ContainerHost<NovelFetchViewState, NovelFetchSideEffect>, ViewModel(),
    ScreenModel {
    private val scope = viewModelScope + Dispatchers.IO

    protected val client = PixivConfig.newAccountFromConfig()

    private var repo: InfinityRepository<Novel>? = null


    override val container: Container<NovelFetchViewState, NovelFetchSideEffect> =
        container(NovelFetchViewState.Loading) {
            initNovel()
        }

    private fun Sequence<Novel>.filterByUserConfig() = this
        .filter {
            if (AppConfig.filterShortNovel) {
                return@filter it.textLength > AppConfig.filterShortNovelMaxLength
            }
            return@filter true
        }.filter {
            if (AppConfig.filterAiNovel) {
                return@filter !it.isAI
            }
            return@filter true
        }
        .filter {
            if (AppConfig.filterR18GNovel) {
                return@filter it.isR18G
            }
            return@filter true
        }
        .filter {
            if (AppConfig.filterR18Novel) {
                //TODO pixko bug, need fixed
                return@filter !(it.isR18 || it.isR18G)
            }
            return@filter true
        }

    abstract fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Novel>

    fun initNovel(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                NovelFetchViewState.Loading
            }
        }
        repo = initInfinityRepository(scope.coroutineContext)
        reduce {
            NovelFetchViewState.ShowNovelList(
                repo!!.filterByUserConfig().take(20).toList(),
                noMoreData = repo!!.noMoreData
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMoreNovels() = intent {
        runOn<NovelFetchViewState.ShowNovelList> {
            reduce {
                state.copy(
                    novels = state.novels + repo!!.filterByUserConfig().take(20).toList(),
                    noMoreData = repo!!.noMoreData
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun likeNovel(novel: Novel) = intent {
        runOn<NovelFetchViewState.ShowNovelList> {
            val result = kotlin.runCatching {
                client.bookmarkNovel(novel.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(NovelFetchSideEffect.Toast("收藏失败~"))
                return@runOn
            }
            postSideEffect(NovelFetchSideEffect.Toast("收藏成功~"))
            reduce {
                state.copy(
                    novels = state.novels.map {
                        if (it.id == novel.id) {
                            it.copy(isBookmarked = true)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeNovel(novel: Novel) = intent {
        runOn<NovelFetchViewState.ShowNovelList> {
            val result = kotlin.runCatching {
                client.deleteBookmarkNovel(novel.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(NovelFetchSideEffect.Toast("取消收藏失败~"))
                return@runOn
            }
            postSideEffect(NovelFetchSideEffect.Toast("取消收藏成功~"))
            reduce {
                state.copy(
                    novels = state.novels.map {
                        if (it == novel) {
                            it.copy(isBookmarked = false)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }
}

sealed class NovelFetchViewState {
    data object Loading : NovelFetchViewState()
    data class ShowNovelList(
        val novels: List<Novel>,
        val noMoreData: Boolean = false,
        val scrollerState: LazyListState = LazyListState()
    ) : NovelFetchViewState()
}

sealed class NovelFetchSideEffect {
    data class Toast(val msg: String) : NovelFetchSideEffect()
}