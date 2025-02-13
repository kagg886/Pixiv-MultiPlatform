package top.kagg886.pmf.backend.pixiv

import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class InfinityRepository<T>(private val context: CoroutineContext = EmptyCoroutineContext) : Sequence<T> {
    private val container = mutableListOf<T>()
    var noMoreData = false
        private set
    override fun iterator(): Iterator<T> {
        return iterator {
            while (true) {
                if (container.isEmpty()) {
                    val new = runBlocking(context) {
                        runCatching {
                            onFetchList()
                        }.getOrNull()
                    }
                    if (new.isNullOrEmpty()) {
                        break
                    }
                    container.addAll(new)
                }
                yield(container.removeFirst())
            }
            noMoreData = true
        }
    }

    abstract suspend fun onFetchList(): List<T>?
}