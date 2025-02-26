package top.kagg886.pmf.ui.route.main.search.v2.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.trending.TrendingTags
import top.kagg886.pmf.ui.component.SupportListItem
import top.kagg886.pmf.ui.route.main.search.v2.toDisplayString

sealed interface TagPropertiesState {
    data object Loading : TagPropertiesState
    data class Loaded(val tags: List<TrendingTags>) : TagPropertiesState
    data class Failed(val msg: String) : TagPropertiesState
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPropertiesPanel(
    modifier: Modifier = Modifier,
    sort: SearchSort,
    target: SearchTarget,
    tag: TagPropertiesState,

    onSortChange: (SearchSort) -> Unit,
    onTargetChange: (SearchTarget) -> Unit,
    onTagRequestRefresh: () -> Unit,
    onTagClicked: (TrendingTags) -> Unit,
) {
    Column(modifier = modifier) {
        ListItem(
            headlineContent = {
                Text("排序方式")
            },
            supportingContent = {
                FlowRow {
                    for (i in SearchSort.entries) {
                        InputChip(
                            selected = sort == i,
                            onClick = {
                                onSortChange(i)
                            },
                            label = {
                                Text(i.toDisplayString())
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            },
        )
        ListItem(
            headlineContent = {
                Text("搜索模式")
            },
            supportingContent = {
                FlowRow {
                    for (i in SearchTarget.entries) {
                        InputChip(
                            selected = target == i,
                            onClick = {
                                onTargetChange(i)
                            },
                            label = {
                                Text(i.toDisplayString())
                            },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

            },
        )
        SupportListItem(
            trailingContent = {
                IconButton(
                    onClick = { onTagRequestRefresh() },
                    enabled = tag != TagPropertiesState.Loading
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        null
                    )
                }
            },
            headlineContent = {
                Text("热门tag")
            },
            supportingContent = {
                when(tag) {
                    TagPropertiesState.Loading -> {
                        LinearProgressIndicator()
                    }
                    is TagPropertiesState.Loaded -> {
                        FlowRow {
                            for (unit in tag.tags) {
                                AssistChip(
                                    onClick = { onTagClicked(unit) },
                                    label = {
                                        Column {
                                            Text(unit.tag.name)
                                            unit.tag.translatedName?.let {
                                                Text("($it)", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                    }
                    is TagPropertiesState.Failed ->  Text("加载失败")
                }
            },
        )
    }

}
