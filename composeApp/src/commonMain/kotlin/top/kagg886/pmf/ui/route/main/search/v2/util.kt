package top.kagg886.pmf.ui.route.main.search.v2

import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchSort.*
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.SearchTarget.*

fun SearchTarget.toDisplayString() = when (this) {
    EXACT_MATCH_FOR_TAGS -> "匹配精确tag"
    PARTIAL_MATCH_FOR_TAGS -> "匹配模糊tag"
    TITLE_AND_CAPTION -> "匹配标题简介(仅插画/作者)"
    TEXT -> "正文匹配(仅小说)"
    KEYWORD -> "关键词(仅小说)"
}

fun SearchSort.toDisplayString() = when (this) {
    DATE_DESC -> "时间降序"
    DATE_ASC -> "时间升序"
    POPULAR_DESC -> "热度降序"
}
