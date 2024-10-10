package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.bookmarkIllust
import top.kagg886.pixko.module.illust.deleteBookmarkIllust
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.IllustHistory
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container

class IllustDetailViewModel : ContainerHost<IllustDetailViewState, IllustDetailSideEffect>, ViewModel(), ScreenModel,
    KoinComponent {
    override val container: Container<IllustDetailViewState, IllustDetailSideEffect> =
        container(IllustDetailViewState.Loading)
    private val token by inject<PixivTokenStorage>()
    private val client = PixivAccountFactory.newAccountFromConfig {
        storage = token
    }

    fun loadByIllustId(id: Long, silent: Boolean = true) = intent {
        if (silent) {
            reduce { IllustDetailViewState.Loading }
        }
        val illust = kotlin.runCatching {
            client.getIllustDetail(id)
        }
        if (illust.isFailure) {
            if (silent) {
                reduce { IllustDetailViewState.Error }
            }
            return@intent
        }
        reduce { IllustDetailViewState.Success(illust.getOrThrow()) }
        saveDataBase()
    }

    fun loadByIllustBean(illust: Illust) = intent {
        reduce {
            IllustDetailViewState.Loading
        }
        reduce { IllustDetailViewState.Success(illust) }
        intent {
            if (illust.contentImages == null) {
                loadByIllustId(illust.id.toLong(), false)
            }
        }
        if (illust.contentImages != null) {
            saveDataBase()
        }
    }

    private val database by inject<AppDatabase>()
    @OptIn(OrbitExperimental::class)
    private fun saveDataBase() = intent {
        runOn<IllustDetailViewState.Success> {
            database.illustHistoryDAO().insert(
                IllustHistory(
                    id = state.illust.id,
                    illust = state.illust,
                    createTime = System.currentTimeMillis()
                )
            )
        }
    }

    @OptIn(OrbitExperimental::class)
    fun likeIllust() = intent {
        runOn<IllustDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.bookmarkIllust(state.illust.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustDetailSideEffect.Toast("收藏失败~"))
                return@runOn
            }
            reduce {
                state.copy(
                    illust = state.illust.copy(isBookMarked = true)
                )
            }
            postSideEffect(IllustDetailSideEffect.Toast("收藏成功~"))
        }
    }

    @OptIn(OrbitExperimental::class)
    fun disLikeIllust() = intent {
        runOn<IllustDetailViewState.Success> {
            val result = kotlin.runCatching {
                client.deleteBookmarkIllust(state.illust.id.toLong())
            }

            if (result.isFailure || result.getOrNull() == false) {
                postSideEffect(IllustDetailSideEffect.Toast("取消收藏失败~"))
                return@runOn
            }
            reduce {
                state.copy(
                    illust = state.illust.copy(isBookMarked = false)
                )
            }
            postSideEffect(IllustDetailSideEffect.Toast("取消收藏成功~"))
        }
    }
}

sealed class IllustDetailViewState {
    data object Loading : IllustDetailViewState()
    data object Error : IllustDetailViewState()
    data class Success(val illust: Illust) : IllustDetailViewState()
}

sealed class IllustDetailSideEffect {
    data class Toast(val msg: String) : IllustDetailSideEffect()
}