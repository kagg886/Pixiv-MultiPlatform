package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.NovelData
import top.kagg886.pixko.module.novel.getNovelContent
import top.kagg886.pixko.module.novel.getNovelDetail
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.NovelHistory
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container

class NovelDetailViewModel(val id: Long) : ViewModel(), ScreenModel,
    ContainerHost<NovelDetailViewState, NovelDetailSideEffect>, KoinComponent {
    override val container: Container<NovelDetailViewState, NovelDetailSideEffect> =
        container(NovelDetailViewState.Loading) {
            loadByNovelId(id)
        }
    private val token by inject<PixivTokenStorage>()
    private val client = PixivAccountFactory.newAccountFromConfig {
        storage = token
    }

    private val database by inject<AppDatabase>()

    fun loadByNovelId(id: Long) = intent {
        reduce {
            NovelDetailViewState.Loading
        }
        val result = kotlin.runCatching {
            client.getNovelDetail(id) to client.getNovelContent(id)
        }
        if (result.isFailure) {
            reduce { NovelDetailViewState.Error }
            return@intent
        }
        val (detail, content) = result.getOrThrow()
        reduce { NovelDetailViewState.Success(detail, content) }
        database.novelHistoryDAO().insert(NovelHistory(id, detail, System.currentTimeMillis()))
    }

    fun getIllustLink(value: Long, num: Int = 0): MutableState<String> {
        val state = mutableStateOf("")
        viewModelScope.launch {
            state.value = client.getIllustDetail(value).contentImages!!.getOrNull(num) ?: ""
        }
        return state
    }
}

sealed class NovelDetailViewState {
    data object Loading : NovelDetailViewState()
    data object Error : NovelDetailViewState()
    data class Success(val novel: Novel, val content: NovelData) : NovelDetailViewState()
}

sealed class NovelDetailSideEffect {
    data class Toast(val msg: String) : NovelDetailSideEffect()
}