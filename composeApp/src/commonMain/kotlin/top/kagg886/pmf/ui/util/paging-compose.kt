package top.kagg886.pmf.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext

class LazyPagingItems<T : Any>(private val flow: Flow<PagingData<T>>) {
    private val mainDispatcher = Dispatchers.Main

    private val pagingDataPresenter = object : PagingDataPresenter<T>(
        mainContext = mainDispatcher,
        cachedPagingData = if (flow is SharedFlow<PagingData<T>>) flow.replayCache.firstOrNull() else null,
    ) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
            updateItemSnapshotList()
        }
    }

    var itemSnapshotList by mutableStateOf(pagingDataPresenter.snapshot())
        private set

    val itemCount: Int
        get() = itemSnapshotList.size

    private fun updateItemSnapshotList() {
        itemSnapshotList = pagingDataPresenter.snapshot()
    }

    operator fun get(index: Int): T? {
        pagingDataPresenter[index] // this registers the value load
        return itemSnapshotList[index]
    }

    fun peek(index: Int): T? = itemSnapshotList[index]

    fun retry() = pagingDataPresenter.retry()

    fun refresh() = pagingDataPresenter.refresh()

    var loadState: CombinedLoadStates by
        mutableStateOf(
            pagingDataPresenter.loadStateFlow.value
                ?: CombinedLoadStates(
                    refresh = InitialLoadStates.refresh,
                    prepend = InitialLoadStates.prepend,
                    append = InitialLoadStates.append,
                    source = InitialLoadStates,
                ),
        )
        private set

    suspend fun collectLoadState() {
        pagingDataPresenter.loadStateFlow.filterNotNull().collect { loadState = it }
    }

    suspend fun collectPagingData() {
        flow.collectLatest { pagingDataPresenter.collectFrom(it) }
    }
}

private val IncompleteLoadState = LoadState.NotLoading(false)
private val InitialLoadStates = LoadStates(LoadState.Loading, IncompleteLoadState, IncompleteLoadState)

@Composable
fun <T : Any> Flow<PagingData<T>>.collectAsLazyPagingItems(
    context: CoroutineContext = EmptyCoroutineContext,
): LazyPagingItems<T> {
    val lazyPagingItems = remember(this) { LazyPagingItems(this) }

    LaunchedEffect(lazyPagingItems) {
        if (context == EmptyCoroutineContext) {
            lazyPagingItems.collectPagingData()
        } else {
            withContext(context) { lazyPagingItems.collectPagingData() }
        }
    }

    LaunchedEffect(lazyPagingItems) {
        if (context == EmptyCoroutineContext) {
            lazyPagingItems.collectLoadState()
        } else {
            withContext(context) { lazyPagingItems.collectLoadState() }
        }
    }

    return lazyPagingItems
}
