package top.kagg886.pmf.backend.pixiv

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.runBlocking
import top.kagg886.pmf.util.logger

abstract class InfinityRepository<T>(private val context: CoroutineContext = EmptyCoroutineContext) : Sequence<T> {
    private val container = mutableListOf<T>()
    var noMoreData = false
        private set
    override fun iterator(): Iterator<T> = iterator {
        while (true) {
            if (container.isEmpty()) {
                val new = runBlocking(context) {
                    try {
                        onFetchList()
                    } catch (e: Throwable) {
                        logger.w("fetch failed", e)
                        null
                    }
                }
                if (new.isNullOrEmpty()) {
                    break
                }
                container.addAll(new)
            }
            yield(container.removeAt(0))
        }
        noMoreData = true
    }

    abstract suspend fun onFetchList(): List<T>?
}
