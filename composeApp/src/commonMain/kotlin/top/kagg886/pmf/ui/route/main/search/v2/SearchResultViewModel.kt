package top.kagg886.pmf.ui.route.main.search.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.SearchHistory
import top.kagg886.pmf.ui.route.main.search.SearchResultIllustModel
import top.kagg886.pmf.ui.route.main.search.SearchResultNovelModel
import top.kagg886.pmf.ui.route.main.search.SearchResultUserModel
import top.kagg886.pmf.ui.util.AuthorFetchViewModel
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.container

class SearchResultViewModel(
    private val keyword: List<String>,
    private val sort: SearchSort,
    private val target: SearchTarget
) : ViewModel(), ScreenModel, KoinComponent, ContainerHost<SearchResultState, SearchResultSideEffect> {

    private val database by inject<AppDatabase>()
    
    override val container: Container<SearchResultState, SearchResultSideEffect> = container(
        initialState = SearchResultState(
            keyword = keyword,
            sort = sort,
            target = target,
            illustRepo = null,
            novelRepo = null,
            authorRepo = null
        )
    ) {
        val (illust, novel, author) = calcThreeRepo(keyword, sort, target)
        
        reduce {
            state.copy(
                illustRepo = illust,
                novelRepo = novel,
                authorRepo = author
            )
        }
        
        saveHistoryIfConfigOn(keyword, sort, target)
    }
    
    private fun saveHistoryIfConfigOn(tag: List<String>, sort: SearchSort, target: SearchTarget) {
        if (!AppConfig.recordSearchHistory) {
            return
        }
        viewModelScope.launch {
            database.searchHistoryDAO().insert(
                SearchHistory(
                    initialSort = sort,
                    initialTarget = target,
                    keyword = tag
                )
            )
        }
    }
    
    private fun calcThreeRepo(
        keyWord: List<String>,
        sort: SearchSort,
        target: SearchTarget,
    ): Triple<IllustFetchViewModel?, NovelFetchViewModel?, AuthorFetchViewModel?> {
        val allText = keyWord.joinToString(" ")
        var illustRepo: IllustFetchViewModel? = null
        var novelRepo: NovelFetchViewModel? = null
        var authorRepo: AuthorFetchViewModel? = null

        when (target) {
            SearchTarget.EXACT_MATCH_FOR_TAGS -> {
                illustRepo = SearchResultIllustModel(allText, target, sort)
                novelRepo = SearchResultNovelModel(allText, target, sort)
                authorRepo = SearchResultUserModel(allText)
            }

            SearchTarget.PARTIAL_MATCH_FOR_TAGS -> {
                illustRepo = SearchResultIllustModel(allText, target, sort)
                novelRepo = SearchResultNovelModel(allText, target, sort)
                authorRepo = SearchResultUserModel(allText)
            }

            SearchTarget.TITLE_AND_CAPTION -> {
                illustRepo = SearchResultIllustModel(allText, target, sort)
                authorRepo = SearchResultUserModel(allText)
            }

            SearchTarget.TEXT -> {
                novelRepo = SearchResultNovelModel(allText, target, sort)
            }

            SearchTarget.KEYWORD -> {
                novelRepo = SearchResultNovelModel(allText, target, sort)
            }
        }

        return Triple(illustRepo, novelRepo, authorRepo)
    }
}

data class SearchResultState(
    val keyword: List<String>,
    val sort: SearchSort,
    val target: SearchTarget,
    val illustRepo: IllustFetchViewModel?,
    val novelRepo: NovelFetchViewModel?,
    val authorRepo: AuthorFetchViewModel?
)

sealed interface SearchResultSideEffect {
    data class Toast(val message: String) : SearchResultSideEffect
} 