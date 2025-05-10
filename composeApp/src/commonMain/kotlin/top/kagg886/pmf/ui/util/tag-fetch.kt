package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.user.*
import top.kagg886.pmf.backend.pixiv.PixivConfig

class TagsFetchViewModel(
    val restrict: UserLikePublicity = UserLikePublicity.PUBLIC,
    val tagsType: FavoriteTagsType = FavoriteTagsType.Illust,
) : ContainerHost<TagsFetchViewState, TagsFetchSideEffect>, ViewModel(), ScreenModel {
    private val client = PixivConfig.newAccountFromConfig()
    private val refreshSignal = MutableSharedFlow<Unit>()

    val data = merge(flowOf(Unit), refreshSignal).flatMapLatest {
        flowOf(30) { p -> p.page { i -> client.getAllFavoriteTags(restrict, tagsType, i) } }
    }.cachedIn(viewModelScope)

    fun refresh() = intent { refreshSignal.emit(Unit) }

    override val container: Container<TagsFetchViewState, TagsFetchSideEffect> = container(TagsFetchViewState())

    @OptIn(OrbitExperimental::class)
    fun selectTags(tags: FavoriteTags) = intent {
        runOn<TagsFetchViewState> {
            reduce { state.copy(selectedTagsFilter = TagFilter.FilterWithTag(Tag(tags.name))) }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun clearTags() = intent {
        runOn<TagsFetchViewState> {
            reduce { state.copy(selectedTagsFilter = TagFilter.NoFilter) }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun selectNonTargetTags() = intent {
        runOn<TagsFetchViewState> {
            reduce { state.copy(selectedTagsFilter = TagFilter.FilterWithoutTagged) }
        }
    }
}

data class TagsFetchViewState(
    val scrollerState: LazyListState = LazyListState(),
    val selectedTagsFilter: TagFilter = TagFilter.NoFilter,
)

sealed class TagsFetchSideEffect {
    data class Toast(val msg: String) : TagsFetchSideEffect()
}
