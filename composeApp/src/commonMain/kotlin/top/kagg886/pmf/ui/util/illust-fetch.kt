package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import arrow.core.identity
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.bookmark_failed
import top.kagg886.pmf.bookmark_success
import top.kagg886.pmf.un_bookmark_failed
import top.kagg886.pmf.un_bookmark_success

private typealias F = (PagingData<Illust>) -> PagingData<Illust>

abstract class IllustFetchViewModel : ContainerHost<IllustFetchViewState, IllustFetchSideEffect>, ViewModel(), ScreenModel {
    protected val client = PixivConfig.newAccountFromConfig()
    private val signal = MutableSharedFlow<F>()

    override val container: Container<IllustFetchViewState, IllustFetchSideEffect> by lazy {
        container(IllustFetchViewState())
    }

    abstract fun source(): Flow<PagingData<Illust>>

    val data = merge(flowOf(::identity), signal).flatMapLatest { f ->
        source().map { data ->
            data
                .filter { !it.isLimited }
                .filterNot { AppConfig.filterAi && it.isAI }
                .filterNot { AppConfig.filterR18G && it.isR18G }
                .filterNot { AppConfig.filterR18 && it.isR18 }
        }.map(f)
    }.cachedIn(viewModelScope)

    fun refresh() = intent { signal.emit(::identity) }

    @OptIn(OrbitExperimental::class)
    fun likeIllust(
        illust: Illust,
        visibility: BookmarkVisibility = BookmarkVisibility.PUBLIC,
        tags: List<Tag>? = null,
    ) = intent {
        runOn<IllustFetchViewState> {
            val result = runCatching {
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
            signal.emit { data ->
                data.map {
                    if (it.id == illust.id) {
                        it.copy(isBookMarked = true)
                    } else {
                        it
                    }
                }
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeIllust(illust: Illust) = intent {
        runOn<IllustFetchViewState> {
            val result = kotlin.runCatching {
                client.deleteBookmarkIllust(illust.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustFetchSideEffect.Toast(getString(Res.string.un_bookmark_failed)))
                return@runOn
            }
            postSideEffect(IllustFetchSideEffect.Toast(getString(Res.string.un_bookmark_success)))
            signal.emit { data ->
                data.map {
                    if (it == illust) {
                        it.copy(isBookMarked = false)
                    } else {
                        it
                    }
                }
            }
        }
    }
}

data class IllustFetchViewState(val scrollerState: LazyStaggeredGridState = LazyStaggeredGridState())

sealed class IllustFetchSideEffect {
    data class Toast(val msg: String) : IllustFetchSideEffect()
}

inline fun <T : Any> PagingData<T>.filterNot(crossinline f: suspend (T) -> Boolean) = filter { v -> !f(v) }
