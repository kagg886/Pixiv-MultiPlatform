package top.kagg886.pmf.ui.route.main.search

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.search.searchTag
import top.kagg886.pixko.module.trending.TrendingTags
import top.kagg886.pixko.module.trending.getRecommendTags
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container

class SearchViewModel : ContainerHost<SearchViewState, Nothing>, ViewModel(), ScreenModel, KoinComponent {
    private val token by inject<PixivTokenStorage>()
    private val client = PixivAccountFactory.newAccountFromConfig {
        this.storage = token
    }

    override val container: Container<SearchViewState, Nothing> = container(SearchViewState.NonLoading) {
        val tag = client.getRecommendTags()
        reduce {
            SearchViewState.EmptySearch(tag)
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
                SearchViewState.EmptySearch(tagCache)
            }
            return@intent
        }
        val t = client.searchTag(key)
        reduce {
            SearchViewState.KeyWordSearch(t)
        }
    }
}

sealed class SearchViewState {
    data object NonLoading : SearchViewState()
    data class EmptySearch(val tag: List<TrendingTags>) : SearchViewState()
    data class KeyWordSearch(val key: List<Tag>) : SearchViewState()
}