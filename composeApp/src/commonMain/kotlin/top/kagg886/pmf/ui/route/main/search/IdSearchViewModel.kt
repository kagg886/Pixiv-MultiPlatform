package top.kagg886.pmf.ui.route.main.search

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.getNovelDetail
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserInfo
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.container

class IdSearchViewModel(val id: Long) : ScreenModel, KoinComponent,
    ContainerHost<IdSearchViewState, IdSearchSideEffect>, ViewModel() {
    override val container: Container<IdSearchViewState, IdSearchSideEffect> = container(IdSearchViewState.Loading) {
        search()
    }

    private val client = PixivConfig.newAccountFromConfig()

    fun search() = intent {
        reduce {
            IdSearchViewState.Loading
        }
        val state = coroutineScope {
            val illustResult = async {
                kotlin.runCatching {
                    client.getIllustDetail(id)
                }.getOrNull()
            }
            val novelResult = async {
                kotlin.runCatching {
                    client.getNovelDetail(id)
                }.getOrNull()
            }
            val userResult = async {
                kotlin.runCatching {
                    client.getUserInfo(id.toInt())
                }.getOrNull()
            }
            IdSearchViewState.LoadSuccess(
                illustResult.await(),
                novelResult.await(),
                userResult.await()
            )
        }
        reduce {
            state
        }
    }
}

sealed interface IdSearchViewState {
    data object Loading : IdSearchViewState
    data class LoadSuccess(
        val illust: Illust?,
        val novel: Novel?,
        val user: UserInfo?
    ): IdSearchViewState
}

sealed interface IdSearchSideEffect
