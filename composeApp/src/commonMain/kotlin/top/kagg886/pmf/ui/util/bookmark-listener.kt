package top.kagg886.pmf.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.paging.PagingData
import androidx.paging.map
import arrow.core.identity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.runningReduce
import top.kagg886.pixko.User
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.novel.Novel

private typealias F<T> = (T) -> T
private typealias FDF<T> = F<Flow<PagingData<T>>>
fun <T : Any> Flow<F<T>>.compose() = runningReduce { a, b -> { v -> b(a(v)) } }

class Router<T : Any> {
    val flow = MutableSharedFlow<F<T>>()
    suspend fun push(f: (T) -> T) = flow.emit(f)
    val intercept: FDF<T> = { src -> merge(flowOf(::identity), flow).compose().flatMapLatest { f -> src.map { d -> d.map(f) } } }

    @Composable
    fun collectLatest(t: T) = remember { flow.runningFold(t) { t, f -> f(t) } }.collectAsState(t)
}

val illustRouter = Router<Illust>()
suspend fun Illust.notifyDislike() = illustRouter.push { i -> if (i.id == id) i.copy(isBookMarked = false, totalBookmarks = i.totalBookmarks - 1) else i }
suspend fun Illust.notifyLike() = illustRouter.push { i -> if (i.id == id) i.copy(isBookMarked = true, totalBookmarks = i.totalBookmarks + 1) else i }

val novelRouter = Router<Novel>()
val userRouter = Router<User>()
