package top.kagg886.pmf.ui.route.main.bookmark

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustResult
import top.kagg886.pixko.module.illust.NovelResult
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.user.*
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.container

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

class BookmarkIllustViewModel(
    val restrict: UserLikePublicity = UserLikePublicity.PUBLIC,
    override val tagFilter: TagFilter = TagFilter.NoFilter,
) : IllustFetchViewModel(), CanAccessTagFilterViewModel {
    private val id = PixivConfig.pixiv_user!!.userId
    override fun initInfinityRepository(): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>() {
            private var ctx: IllustResult? = null
            override suspend fun onFetchList(): List<Illust>? {
                ctx = if (ctx == null) {
                    client.getUserLikeIllust(
                        id,
                        restrict,
                        tagFilter,
                    )
                } else {
                    client.getUserLikeIllustNext(ctx!!)
                }
                return ctx?.illusts
            }
        }
    }
}

class BookmarkNovelViewModel(
    private val restrict: UserLikePublicity = UserLikePublicity.PUBLIC,
    override val tagFilter: TagFilter = TagFilter.NoFilter,
) : NovelFetchViewModel(), CanAccessTagFilterViewModel {
    private val id = PixivConfig.pixiv_user!!.userId
    override fun initInfinityRepository(): InfinityRepository<Novel> {
        return object : InfinityRepository<Novel>() {
            private var ctx: NovelResult? = null
            override suspend fun onFetchList(): List<Novel>? {
                ctx = if (ctx == null) {
                    client.getUserLikeNovel(
                        id,
                        restrict,
                        tagFilter,
                    )
                } else {
                    client.getUserLikeNovelNext(ctx!!)
                }
                return ctx?.novels
            }
        }
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
