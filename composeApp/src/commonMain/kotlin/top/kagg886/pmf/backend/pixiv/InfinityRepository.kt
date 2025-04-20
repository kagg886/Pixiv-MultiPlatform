package top.kagg886.pmf.backend.pixiv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import top.kagg886.pmf.util.logger

abstract class InfinityRepository<T> : Flow<T> {
    var noMoreData = false
        private set

    private val flow = with(ArrayDeque<T>()) {
        flow {
            while (true) {
                if (isEmpty()) {
                    val new = try {
                        onFetchList()?.ifEmpty { null } ?: break
                    } catch (e: Throwable) {
                        logger.w("fetch failed", e)
                        break
                    }
                    addAll(new)
                }
                emit(removeFirst())
            }
            noMoreData = true
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) = flow.flowOn(Dispatchers.IO).collect(collector)

    abstract suspend fun onFetchList(): List<T>?
}
