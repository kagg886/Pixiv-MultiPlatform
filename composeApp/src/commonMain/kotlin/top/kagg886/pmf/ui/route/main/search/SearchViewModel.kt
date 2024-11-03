package top.kagg886.pmf.ui.route.main.search

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
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
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.NovelHistory
import top.kagg886.pmf.backend.database.dao.SearchHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.container

class SearchViewModel : ContainerHost<SearchViewState, Nothing>, ViewModel(), ScreenModel, KoinComponent {
    private val client = PixivConfig.newAccountFromConfig()

    private val database by inject<AppDatabase>()

    private val history = database.searchHistoryDAO().allFlow()

    override val container: Container<SearchViewState, Nothing> = container(SearchViewState.NonLoading) {
        val tag = client.getRecommendTags()
        reduce {
            SearchViewState.EmptySearch(tag,history)
        }
    }
    private val tagCache by lazy {
        runBlocking {
            client.getRecommendTags()
        }
    }

    fun searchTag(key: String) = intent {
        reduce {
            SearchViewState.NonLoading
        }
        if (key.isBlank()) {
            reduce {
                SearchViewState.EmptySearch(tagCache,history)
            }
            return@intent
        }
        val t = client.searchTag(key)
        reduce {
            SearchViewState.KeyWordSearch(t,history)
        }
    }

    fun saveSearchHistory(
        sort: SearchSort,
        target: SearchTarget,
        key: String,
        tab: SearchTab
    ) = intent {
        database.searchHistoryDAO().insert(
            SearchHistory(
                -1,
                sort,
                target,
                key,
                tab
            )
        )
    }

    fun deleteSearchHistory(history: SearchHistory) = intent {
        database.searchHistoryDAO().delete(history)
    }
}

sealed interface SearchViewState {
    data object NonLoading : SearchViewState
    data class EmptySearch(val tag: List<TrendingTags>, override val history: Flow<List<SearchHistory>>) : SearchViewState,CanAccessHistory
    data class KeyWordSearch(val key: List<Tag>, override val history: Flow<List<SearchHistory>>) : SearchViewState,CanAccessHistory
}

interface CanAccessHistory {
    val history: Flow<List<SearchHistory>>
}