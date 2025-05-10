package top.kagg886.pmf.ui.util

import androidx.paging.PagingData
import androidx.paging.map
import arrow.core.identity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningReduce
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.novel.Novel

private typealias F<T> = (PagingData<T>) -> PagingData<T>
fun <T : Any> Flow<F<T>>.compose() = runningReduce { a, b -> { v -> b(a(v)) } }

class Router<T : Any> {
    val flow = MutableSharedFlow<F<T>>()
    suspend inline fun push(crossinline f: (T) -> T) = flow.emit { d -> d.map { t -> f(t) } }
    fun intercept(src: Flow<PagingData<T>>) = run {
        merge(flowOf(::identity), flow.filterIsInstance<F<T>>()).compose().flatMapLatest { f -> src.map(f) }
    }
}

val illustRouter = Router<Illust>()
val novelRouter = Router<Novel>()
