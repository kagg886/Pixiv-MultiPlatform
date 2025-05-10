package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.identity
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningReduce
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.BookmarkVisibility
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.bookmarkNovel
import top.kagg886.pixko.module.novel.deleteBookmarkNovel
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.bookmark_failed
import top.kagg886.pmf.bookmark_success
import top.kagg886.pmf.un_bookmark_failed
import top.kagg886.pmf.un_bookmark_success

private typealias FN = (PagingData<Novel>) -> PagingData<Novel>

abstract class NovelFetchViewModel : ContainerHost<NovelFetchViewState, NovelFetchSideEffect>, ViewModel(), ScreenModel {
    protected val client = PixivConfig.newAccountFromConfig()
    private val refreshSignal = MutableSharedFlow<Unit>()
    private val transforms = MutableSharedFlow<FN>()

    override val container: Container<NovelFetchViewState, NovelFetchSideEffect> = container(NovelFetchViewState())
    abstract fun source(): Flow<PagingData<Novel>>

    fun Novel.block() = with(AppConfig) {
        val a = filterShortNovel && textLength <= filterShortNovelMaxLength
        val b = filterLongTag && tags.any { it.name.length > filterLongTagMinLength }
        val c = filterAiNovel && isAI
        val d = filterR18GNovel && isR18G
        val e = filterR18Novel && (isR18 || isR18G)
        a || b || c || d || e
    }

    val data = merge(flowOf(Unit), refreshSignal).flatMapLatest {
        source().cachedIn(viewModelScope).let { cached ->
            merge(flowOf(::identity), transforms).runningReduce { a, b -> { v -> b(a(v)) } }.flatMapLatest { f ->
                cached.map { data -> data.filterNot { i -> i.block() } }.map(f)
            }
        }
    }.cachedIn(viewModelScope)

    fun refresh() = intent { refreshSignal.emit(Unit) }

    @OptIn(OrbitExperimental::class)
    fun likeNovel(
        novel: Novel,
        visibility: BookmarkVisibility = BookmarkVisibility.PUBLIC,
        tags: List<Tag>? = null,
    ) = intent {
        runOn<NovelFetchViewState> {
            val result = kotlin.runCatching {
                client.bookmarkNovel(novel.id.toLong()) {
                    this.visibility = visibility
                    this.tags = tags
                }
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(NovelFetchSideEffect.Toast(getString(Res.string.bookmark_failed)))
                return@runOn
            }
            postSideEffect(NovelFetchSideEffect.Toast(getString(Res.string.bookmark_success)))
            transforms.emit { data ->
                data.map {
                    if (it.id == novel.id) {
                        it.copy(isBookmarked = true)
                    } else {
                        it
                    }
                }
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeNovel(novel: Novel) = intent {
        runOn<NovelFetchViewState> {
            val result = kotlin.runCatching {
                client.deleteBookmarkNovel(novel.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(NovelFetchSideEffect.Toast(getString(Res.string.un_bookmark_failed)))
                return@runOn
            }
            postSideEffect(NovelFetchSideEffect.Toast(getString(Res.string.un_bookmark_success)))
            transforms.emit { data ->
                data.map {
                    if (it == novel) {
                        it.copy(isBookmarked = false)
                    } else {
                        it
                    }
                }
            }
        }
    }
}

data class NovelFetchViewState(val scrollerState: LazyListState = LazyListState())

sealed class NovelFetchSideEffect {
    data class Toast(val msg: String) : NovelFetchSideEffect()
}
