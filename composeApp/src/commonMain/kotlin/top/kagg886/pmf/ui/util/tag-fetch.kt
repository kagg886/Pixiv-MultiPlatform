package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.user.*
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig

class TagsFetchViewModel(
    val restrict: UserLikePublicity = UserLikePublicity.PUBLIC,
    val tagsType: FavoriteTagsType = FavoriteTagsType.Illust,
) : ContainerHost<TagsFetchViewState, TagsFetchSideEffect>, ViewModel(), ScreenModel {
    private val client = PixivConfig.newAccountFromConfig()

    private lateinit var repo: InfinityRepository<FavoriteTags>

    override val container: Container<TagsFetchViewState, TagsFetchSideEffect> =
        container(TagsFetchViewState.Loading) {
            initTags()
        }

    fun initInfinityRepository(): InfinityRepository<FavoriteTags> = object : InfinityRepository<FavoriteTags>() {
        private var page: Int = 1
        override suspend fun onFetchList(): List<FavoriteTags> {
            val res = client.getAllFavoriteTags(restrict, tagsType, page)
            page++
            return res
        }
    }

    fun initTags(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                TagsFetchViewState.Loading
            }
        }
        repo = initInfinityRepository()
        val list = repo.take(20).toList()
        reduce { TagsFetchViewState.ShowTagsList(list, noMoreData = repo.noMoreData) }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMoreTags() = intent {
        runOn<TagsFetchViewState.ShowTagsList> {
            val list = state.data + repo.take(20).toList()
            reduce { state.copy(data = list, noMoreData = repo.noMoreData) }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun selectTags(tags: FavoriteTags) = intent {
        runOn<TagsFetchViewState.ShowTagsList> {
            reduce {
                state.copy(
                    selectedTagsFilter = TagFilter.FilterWithTag(Tag(tags.name)),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun clearTags() = intent {
        runOn<TagsFetchViewState.ShowTagsList> {
            reduce {
                state.copy(
                    selectedTagsFilter = TagFilter.NoFilter,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun selectNonTargetTags() = intent {
        runOn<TagsFetchViewState.ShowTagsList> {
            reduce {
                state.copy(
                    selectedTagsFilter = TagFilter.FilterWithoutTagged,
                )
            }
        }
    }
}

sealed class TagsFetchViewState {
    data object Loading : TagsFetchViewState()
    data class ShowTagsList(
        val data: List<FavoriteTags>,
        val noMoreData: Boolean = false,
        val scrollerState: LazyListState = LazyListState(),
        val selectedTagsFilter: TagFilter = TagFilter.NoFilter,
    ) : TagsFetchViewState()
}

sealed class TagsFetchSideEffect {
    data class Toast(val msg: String) : TagsFetchSideEffect()
}
