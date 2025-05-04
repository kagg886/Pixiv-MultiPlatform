package top.kagg886.pmf.ui.route.main.search.v2

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchSort.DATE_ASC
import top.kagg886.pixko.module.search.SearchSort.DATE_DESC
import top.kagg886.pixko.module.search.SearchSort.POPULAR_DESC
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.SearchTarget.EXACT_MATCH_FOR_TAGS
import top.kagg886.pixko.module.search.SearchTarget.KEYWORD
import top.kagg886.pixko.module.search.SearchTarget.PARTIAL_MATCH_FOR_TAGS
import top.kagg886.pixko.module.search.SearchTarget.TEXT
import top.kagg886.pixko.module.search.SearchTarget.TITLE_AND_CAPTION
import top.kagg886.pmf.Res
import top.kagg886.pmf.match_exact_tag
import top.kagg886.pmf.match_fuzzy_tag
import top.kagg886.pmf.match_keyword
import top.kagg886.pmf.match_text
import top.kagg886.pmf.match_title_caption
import top.kagg886.pmf.sort_date_asc
import top.kagg886.pmf.sort_date_desc
import top.kagg886.pmf.sort_popular_desc

@Composable
fun SearchTarget.toDisplayString() = when (this) {
    EXACT_MATCH_FOR_TAGS -> stringResource(Res.string.match_exact_tag)
    PARTIAL_MATCH_FOR_TAGS -> stringResource(Res.string.match_fuzzy_tag)
    TITLE_AND_CAPTION -> stringResource(Res.string.match_title_caption)
    TEXT -> stringResource(Res.string.match_text)
    KEYWORD -> stringResource(Res.string.match_keyword)
}

@Composable
fun SearchSort.toDisplayString() = when (this) {
    DATE_DESC -> stringResource(Res.string.sort_date_desc)
    DATE_ASC -> stringResource(Res.string.sort_date_asc)
    POPULAR_DESC -> stringResource(Res.string.sort_popular_desc)
}
