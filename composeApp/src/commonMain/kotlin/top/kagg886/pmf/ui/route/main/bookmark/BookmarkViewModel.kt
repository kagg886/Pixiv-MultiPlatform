package top.kagg886.pmf.ui.route.main.bookmark

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cafe.adriel.voyager.core.model.ScreenModel
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustResult
import top.kagg886.pixko.module.illust.NovelResult
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.user.*
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.catch
import top.kagg886.pmf.ui.util.container
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
    override fun source() = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<IllustResult, Illust>() {
            override fun getRefreshKey(state: PagingState<IllustResult, Illust>) = null
            override suspend fun load(params: LoadParams<IllustResult>) = catch {
                params.next(
                    { client.getUserLikeIllust(id, restrict, tagFilter) },
                    { ctx -> client.getUserLikeIllustNext(ctx) },
                    { ctx -> ctx.illusts },
                )
            }
        }
    }.flow
}

class BookmarkNovelViewModel(
    private val restrict: UserLikePublicity = UserLikePublicity.PUBLIC,
    override val tagFilter: TagFilter = TagFilter.NoFilter,
) : NovelFetchViewModel(), CanAccessTagFilterViewModel {
    private val id = PixivConfig.pixiv_user!!.userId
    override fun source() = Pager(PagingConfig(20)) {
        object : PagingSource<NovelResult, Novel>() {
            override fun getRefreshKey(state: PagingState<NovelResult, Novel>) = null
            override suspend fun load(params: LoadParams<NovelResult>) = catch {
                params.next(
                    { client.getUserLikeNovel(id, restrict, tagFilter) },
                    { ctx -> client.getUserLikeNovelNext(ctx) },
                    { ctx -> ctx.novels },
                )
            }
        }
    }.flow
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
