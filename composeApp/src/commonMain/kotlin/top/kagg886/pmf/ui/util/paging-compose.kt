package top.kagg886.pmf.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.PagingState
import androidx.paging.awaitNotLoading
import arrow.core.identity
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext

inline fun <T, reified E : Throwable> Result<T>.except() = onFailure { e -> if (e is E) throw e }

suspend inline fun <K : Any, V : Any, R : LoadResult<K, V>> catch(crossinline f: suspend () -> R) = withContext(Dispatchers.IO) {
    runCatching { f() }.except<R, CancellationException>().fold(::identity) { LoadResult.Error<K, V>(it) }
}

inline fun <K : Any, V : Any> flowOf(pageSize: Int, crossinline f: suspend (LoadParams<K>) -> LoadResult<K, V>) = Pager(PagingConfig(pageSize)) {
    object : PagingSource<K, V>() {
        override fun getRefreshKey(state: PagingState<K, V>) = null
        override suspend fun load(params: LoadParams<K>) = catch { f(params) }
    }
}.flow

val empty = Page(emptyList(), null, null, 0, 0)

@Suppress("UNCHECKED_CAST")
fun <Key : Any, Value : Any> empty() = empty as Page<Key, Value>

suspend inline fun <K : Any, T : Any> LoadParams<K>.next(
    fa: suspend () -> K,
    fb: suspend (K) -> K?,
    t: (K) -> List<T>,
): Page<K, T> {
    val k = key?.let { fb(it) ?: return empty() } ?: fa()
    val l = t(k).takeUnless { it.isEmpty() } ?: return empty()
    return LoadResult.Page(l, null, k)
}

suspend inline fun <T : Any> LoadParams<Int>.page(
    f: suspend (Int) -> List<T>,
): Page<Int, T> {
    val k = key ?: 1
    val r = f(k).takeUnless { it.isEmpty() } ?: return empty()
    return LoadResult.Page(r, if (k > 1) k - 1 else null, k + 1)
}

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

suspend fun <T : Any> LazyPagingItems<T>.awaitNextState() {
    delay(200)
    snapshotFlow { loadState }.awaitNotLoading()
}

inline fun <T, R> Flow<T>.flatMapLatestScoped(crossinline transform: suspend (scope: CoroutineScope, value: T) -> Flow<R>) = transformLatest {
    coroutineScope { emitAll(transform(this, it)) }
}
