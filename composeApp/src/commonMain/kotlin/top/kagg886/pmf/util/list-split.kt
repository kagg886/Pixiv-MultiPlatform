package top.kagg886.pmf.util

import java.util.*

//返回true代表启动分割
inline fun <T> List<T>.splitBy(includeSplitSelf: Boolean = false, block: (T) -> Boolean): List<List<T>> {
    val result = LinkedList<List<T>>()
    var current = LinkedList<T>()
    forEach {
        if (block(it)) {
            result.add(current)
            if (includeSplitSelf) {
                current = LinkedList<T>().apply {
                    add(it)
                }
                result.add(current)
            }
            current = LinkedList()
        } else {
            current.add(it)
        }
    }
    if (current.isNotEmpty()) {
        result.add(current)
    }
    return result
}