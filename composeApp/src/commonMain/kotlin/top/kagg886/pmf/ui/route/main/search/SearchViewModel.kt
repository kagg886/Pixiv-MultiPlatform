package top.kagg886.pmf.ui.route.main.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.searchTag
import top.kagg886.pixko.module.trending.TrendingTags
import top.kagg886.pixko.module.trending.getRecommendTags
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.SearchHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.container

class SearchViewModel : ContainerHost<SearchViewState, Nothing>, ViewModel(), ScreenModel, KoinComponent {
    private val client = PixivConfig.newAccountFromConfig()

    private val database by inject<AppDatabase>()

    private val history = database.searchHistoryDAO().allFlow()

    override val container: Container<SearchViewState, Nothing> = container(SearchViewState.NonLoading) {
        refreshTag()
    }

    private val flow = MutableStateFlow<Result<List<TrendingTags>>?>(null)

    fun refreshTag() = intent {
        viewModelScope.launch {
            flow.emit(null)
            withContext(Dispatchers.IO) {
                flow.emit(
                    kotlin.runCatching {
                        client.getRecommendTags()
                    }
                )
            }
        }
    }

    fun searchTag(key: String) = intent {
        reduce {
            SearchViewState.NonLoading
        }
        if (key.isBlank()) {
            reduce {
                SearchViewState.EmptySearch(flow, history)
            }
            return@intent
        }
        val t = kotlin.runCatching {
            client.searchTag(key)
        }
        if (t.isFailure) {
            reduce {
                SearchViewState.SearchFailed("网络不好呢", history)
            }
            return@intent
        }
        if (t.getOrThrow().isEmpty()) {
            reduce {
                SearchViewState.SearchFailed("没有找到相关标签", history)
            }
            return@intent
        }
        reduce {
            SearchViewState.KeyWordSearch(t.getOrThrow(), history)
        }
    }

    fun saveSearchHistory(
        sort: SearchSort,
        target: SearchTarget,
        key: String,
        tab: SearchTab
    ) = intent {
        if (!AppConfig.recordSearchHistory) {
            return@intent
        }
        database.searchHistoryDAO().insert(
            SearchHistory(
                initialSort = sort,
                initialTarget = target,
                initialKeyWords = key,
                tab = tab,
            )
        )
    }

    fun deleteSearchHistory(history: SearchHistory) = intent {
        database.searchHistoryDAO().delete(history)
    }
}

sealed interface SearchViewState {
    data object NonLoading : SearchViewState
    data class EmptySearch(
        val tag: Flow<Result<List<TrendingTags>>?>,
        override val history: Flow<List<SearchHistory>>
    ) :
        SearchViewState, CanAccessHistory

    data class KeyWordSearch(val key: List<Tag>, override val history: Flow<List<SearchHistory>>) : SearchViewState,
        CanAccessHistory

    data class SearchFailed(val msg: String, override val history: Flow<List<SearchHistory>>) : SearchViewState,
        CanAccessHistory
}

interface CanAccessHistory {
    val history: Flow<List<SearchHistory>>
}