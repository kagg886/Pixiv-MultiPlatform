package top.kagg886.pmf.ui.route.main.bookmark

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.user.*
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.container
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.next

class BookmarkViewModel : ContainerHost<BookmarkViewState, BookmarkSideEffect>, ViewModel(), ScreenModel {
    override val container: Container<BookmarkViewState, BookmarkSideEffect> = container(BookmarkViewState.LoadSuccess())

    @OptIn(OrbitExperimental::class)
    fun selectTagFilter(tagFilter: TagFilter) = intent {
        runOn<BookmarkViewState.LoadSuccess> {
            reduce {
                state.copy(
                    tagFilter = tagFilter,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun selectMode(mode: FavoriteTagsType) = intent {
        runOn<BookmarkViewState.LoadSuccess> {
            reduce {
                state.copy(
                    mode = mode,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun selectPublicity(publicity: UserLikePublicity) = intent {
        runOn<BookmarkViewState.LoadSuccess> {
            reduce {
                state.copy(
                    restrict = publicity,
                )
            }
        }
    }
}

sealed interface CanAccessTagFilterViewModel {
    val tagFilter: TagFilter
}

class BookmarkIllustViewModel(val restrict: UserLikePublicity = UserLikePublicity.PUBLIC, override val tagFilter: TagFilter = TagFilter.NoFilter) : IllustFetchViewModel(), CanAccessTagFilterViewModel {
    private val id = PixivConfig.pixiv_user!!.userId
    override fun source() = flowOf(30) { params ->
        params.next(
            { client.getUserLikeIllust(id, restrict, tagFilter) },
            { ctx -> client.getUserLikeIllustNext(ctx) },
            { ctx -> ctx.illusts },
        )
    }
}

class BookmarkNovelViewModel(
    private val restrict: UserLikePublicity = UserLikePublicity.PUBLIC,
    override val tagFilter: TagFilter = TagFilter.NoFilter,
) : NovelFetchViewModel(), CanAccessTagFilterViewModel {
    private val id = PixivConfig.pixiv_user!!.userId
    override fun source() = flowOf(20) { params ->
        params.next(
            { client.getUserLikeNovel(id, restrict, tagFilter) },
            { ctx -> client.getUserLikeNovelNext(ctx) },
            { ctx -> ctx.novels },
        )
    }
}

sealed interface BookmarkViewState {
    data object Loading : BookmarkViewState

    data class LoadSuccess(
        val restrict: UserLikePublicity = UserLikePublicity.PUBLIC,
        val tagFilter: TagFilter = TagFilter.NoFilter,
        val mode: FavoriteTagsType = FavoriteTagsType.Illust,
    ) : BookmarkViewState
}

sealed class BookmarkSideEffect {
    data object Toast : BookmarkSideEffect()
}
