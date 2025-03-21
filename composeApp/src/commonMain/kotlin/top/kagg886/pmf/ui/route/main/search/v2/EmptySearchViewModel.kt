package top.kagg886.pmf.ui.route.main.search.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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

    override val container: Container<EmptySearchState, EmptySearchSideEffect> = container(
        EmptySearchState(database.searchHistoryDAO().allFlow())
    )

    fun deleteHistory(history: SearchHistory) = intent {
        viewModelScope.launch {
            database.searchHistoryDAO().delete(history)
        }
    }
}

data class EmptySearchState(
    val historyFlow: Flow<List<SearchHistory>>
)

sealed interface EmptySearchSideEffect {
    data class Toast(val message: String) : EmptySearchSideEffect
}
