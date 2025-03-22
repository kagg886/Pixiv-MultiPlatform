package top.kagg886.pmf.ui.route.main.search.v2

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pmf.backend.database.AppDatabase
import top.kagg886.pmf.backend.database.dao.SearchHistory
import top.kagg886.pmf.ui.util.container

class EmptySearchViewModel : ViewModel(), ScreenModel, KoinComponent,
    ContainerHost<EmptySearchState, EmptySearchSideEffect> {

    private val database by inject<AppDatabase>()

    override val container: Container<EmptySearchState, EmptySearchSideEffect> = container(EmptySearchState.Loading) {
        loadHistory()
    }

    private fun loadHistory() = intent {
        reduce {
            EmptySearchState.Loading
        }
        val historyFlow = database.searchHistoryDAO().allFlow()
        reduce {
            EmptySearchState.ShowHistoryList(historyFlow)
        }
    }

    fun deleteHistory(history: SearchHistory) = intent {
        database.searchHistoryDAO().delete(history)
        postSideEffect(EmptySearchSideEffect.Toast("已删除搜索历史"))
    }

    fun clearHistory() = intent {
        database.searchHistoryDAO().clear()
        postSideEffect(EmptySearchSideEffect.Toast("已清空搜索历史"))
    }
}

sealed class EmptySearchState {
    data object Loading : EmptySearchState()
    data class ShowHistoryList(val historyFlow: Flow<List<SearchHistory>>) : EmptySearchState()
}

sealed interface EmptySearchSideEffect {
    data class Toast(val message: String) : EmptySearchSideEffect
}
