package top.kagg886.pmf.ui.route.main.search.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.getNovelDetail
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.SearchTarget.*
import top.kagg886.pixko.module.search.searchTag
import top.kagg886.pixko.module.trending.getRecommendTags
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserInfo
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.SearchHistory
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.route.main.search.SearchResultIllustModel
import top.kagg886.pmf.ui.route.main.search.SearchResultNovelModel
import top.kagg886.pmf.ui.route.main.search.SearchResultUserModel
import top.kagg886.pmf.ui.route.main.search.v2.components.TagPropertiesState
import top.kagg886.pmf.ui.util.AuthorFetchViewModel
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.container

class SearchViewModel(param: SearchParam) : ViewModel(), ScreenModel, KoinComponent,
    ContainerHost<SearchViewState, SearchSideEffect> {

    private val database by inject<AppDatabase>()
    private val client = PixivConfig.newAccountFromConfig()


    override val container: Container<SearchViewState, SearchSideEffect> =
        container(
            when (param) {
                is SearchParam.EmptySearch -> SearchViewState.MainPanel.EmptySearch(
                    database.searchHistoryDAO().allFlow()
                )

                is SearchParam.KeyWordSearch -> {
                    val (a, b, c) = calcThreeRepo(param.tag, param.sort, param.target)
                    saveHistoryIfConfigOn(param.tag, param.sort, param.target)
                    SearchViewState.MainPanel.SearchResult(param.target, param.sort, param.tag, a, b, c)
                }
            }
        )

    private fun saveHistoryIfConfigOn(tag: List<String>, sort: SearchSort, target: SearchTarget) {
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

    @OptIn(OrbitExperimental::class)
    fun deleteHistory(history: SearchHistory) = intent {
        runOn<SearchViewState.MainPanel.EmptySearch> {
            database.searchHistoryDAO().delete(history)
        }
    }

    fun openSearchPanel(
        text: String = "",
        sort: SearchSort = SearchSort.DATE_DESC,
        target: SearchTarget = PARTIAL_MATCH_FOR_TAGS,
        keyword: List<String> = listOf()
    ) = intent {
        reduce {
            SearchViewState.SearchPanel.SettingProperties(
                keyword = MutableStateFlow(keyword),
                text = MutableStateFlow(text),
                sort = MutableStateFlow(sort),
                target = MutableStateFlow(target),
            )
        }
        refreshHotTag()
    }

    @OptIn(OrbitExperimental::class)
    fun closeSearchPanel() = intent {
        runOn<SearchViewState.SearchPanel> {
            reduce {
                SearchViewState.MainPanel.EmptySearch(database.searchHistoryDAO().allFlow())
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun updateProperties(
        sort: SearchSort? = null,
        target: SearchTarget? = null,
    ) = intent {
        runOn<SearchViewState.SearchPanel.SettingProperties> {
            sort?.let {
                val sortFlow = state.sort as MutableStateFlow<SearchSort>
                sortFlow.emit(it)
            }
            target?.let {
                val targetFlow = state.target as MutableStateFlow<SearchTarget>
                targetFlow.emit(it)
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun refreshHotTag() = intent {
        runOn<SearchViewState.SearchPanel.SettingProperties> {
            val flow = state.hotTag as MutableStateFlow<TagPropertiesState>
            intent {
                flow.emit(
                    TagPropertiesState.Loading
                )
                try {
                    flow.emit(
                        TagPropertiesState.Loaded(
                            client.getRecommendTags()
                        )
                    )
                } catch (e: Exception) {
                    flow.emit(
                        TagPropertiesState.Failed(
                            e.toString()
                        )
                    )
                }
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun searchTagOrExactSearch(text: String) = intent {
        runOn<SearchViewState.SearchPanel> {
            with(text.toLongOrNull()) {
                if (this != null && state.keyword.value.isEmpty()) {
                    val (illust, novel, author) = coroutineScope {
                        val a1 = async {
                            try {
                                client.getIllustDetail(this@with)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val a2 = async {
                            try {
                                client.getNovelDetail(this@with)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val a3 = async {
                            try {
                                client.getUserInfo(this@with.toInt())
                            } catch (e: Exception) {
                                null
                            }
                        }
                        Triple(a1.await(), a2.await(), a3.await())
                    }

                    reduce {
                        SearchViewState.SearchPanel.RedirectToPage(
                            illust = illust,
                            novel = novel,
                            user = author,
                            keyword = state.keyword,
                            text = state.text,
                            sort = state.sort,
                            target = state.target,
                        )
                    }
                    return@runOn
                }
            }

            reduce {
                SearchViewState.SearchPanel.Searching(
                    keyword = state.keyword,
                    text = state.text,
                    sort = state.sort,
                    target = state.target,
                )
            }
            try {
                val result = client.searchTag(text)
                reduce {
                    SearchViewState.SearchPanel.SelectTag(
                        tags = result,
                        keyword = state.keyword,
                        text = state.text,
                        sort = state.sort,
                        target = state.target,
                    )
                }
            } catch (e: Exception) {
                reduce {
                    SearchViewState.SearchPanel.SearchingFailed(
                        e.message ?: "未知错误",
                        sort = state.sort,
                        target = state.target,
                        keyword = state.keyword,
                        text = state.text,
                    )
                }
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun selectTag(it: Tag) = intent {
        runOn<SearchViewState.SearchPanel> {
            val keyword = state.keyword as MutableStateFlow<List<String>>
            keyword.emit(keyword.value + it.name)
            state.text.emit("")

            if (state is SearchViewState.SearchPanel.SettingProperties) { //避免无意义动画重载
                return@runOn
            }

            reduce {
                SearchViewState.SearchPanel.SettingProperties(
                    sort = state.sort,
                    target = state.target,
                    keyword = state.keyword,
                    text = state.text,
                )
            }
            refreshHotTag()
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
            EXACT_MATCH_FOR_TAGS -> {
                illustRepo = SearchResultIllustModel(allText, target, sort)
                novelRepo = SearchResultNovelModel(allText, target, sort)
                authorRepo = SearchResultUserModel(allText)
            }

            PARTIAL_MATCH_FOR_TAGS -> {
                illustRepo = SearchResultIllustModel(allText, target, sort)
                novelRepo = SearchResultNovelModel(allText, target, sort)
                authorRepo = SearchResultUserModel(allText)
            }

            TITLE_AND_CAPTION -> {
                illustRepo = SearchResultIllustModel(allText, target, sort)
            }

            TEXT -> {
                novelRepo = SearchResultNovelModel(allText, target, sort)
            }

            KEYWORD -> {
                novelRepo = SearchResultNovelModel(allText, target, sort)
            }
        }

        return Triple(illustRepo, novelRepo, authorRepo)
    }

    fun startSearch(
        keyWord: List<String>,
        sort: SearchSort,
        target: SearchTarget,
    ) = intent {
        val (illustRepo, novelRepo, authorRepo) = calcThreeRepo(keyWord, sort, target)
        saveHistoryIfConfigOn(keyWord, sort, target)
        reduce {
            SearchViewState.MainPanel.SearchResult(
                target,
                sort,
                keyWord,
                illustRepo,
                novelRepo,
                authorRepo
            )
        }
    }
}


sealed interface SearchViewState {

    sealed interface MainPanel : SearchViewState {
        data class EmptySearch(val history: Flow<List<SearchHistory>>) : MainPanel //初始页面
        data class SearchResult(
            val target: SearchTarget,
            val sort: SearchSort,
            val keyword: List<String>,

            val illustRepo: IllustFetchViewModel?,
            val novelRepo: NovelFetchViewModel?,
            val authorRepo: AuthorFetchViewModel?
        ) : MainPanel //展示搜索结果
    }

    sealed interface SearchPanel : SearchViewState {
        val keyword: StateFlow<List<String>>
        val text: MutableStateFlow<String>

        val sort: StateFlow<SearchSort>
        val target: StateFlow<SearchTarget>

        data class SettingProperties(
            val hotTag: StateFlow<TagPropertiesState> = MutableStateFlow(TagPropertiesState.Loading),

            override val sort: StateFlow<SearchSort>,
            override val target: StateFlow<SearchTarget>,
            override val keyword: StateFlow<List<String>>,
            override val text: MutableStateFlow<String>,
        ) : SearchPanel //搜索功能

        data class Searching(
            override val sort: StateFlow<SearchSort>,
            override val target: StateFlow<SearchTarget>,
            override val keyword: StateFlow<List<String>>,
            override val text: MutableStateFlow<String>,
        ) : SearchPanel  //正在搜索


        data class SearchingFailed(
            val msg: String,
            override val sort: StateFlow<SearchSort>,
            override val target: StateFlow<SearchTarget>,
            override val keyword: StateFlow<List<String>>,
            override val text: MutableStateFlow<String>,
        ) : SearchPanel //搜索失败

        data class SelectTag(
            val tags: List<Tag>,

            override val sort: StateFlow<SearchSort>,
            override val target: StateFlow<SearchTarget>,
            override val text: MutableStateFlow<String>,
            override val keyword: StateFlow<List<String>>,
        ) : SearchPanel //搜索到标签

        data class RedirectToPage(
            val illust: Illust?,
            val novel: Novel?,
            val user: UserInfo?,


            override val keyword: StateFlow<List<String>>,
            override val text: MutableStateFlow<String>,
            override val sort: StateFlow<SearchSort>,
            override val target: StateFlow<SearchTarget>,
        ) : SearchPanel //搜索到内容
    }
}

sealed interface SearchSideEffect {

}