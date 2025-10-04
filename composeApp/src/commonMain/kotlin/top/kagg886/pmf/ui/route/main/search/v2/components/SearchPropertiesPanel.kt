package top.kagg886.pmf.ui.route.main.search.v2.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.trending.TrendingTags
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.component.SupportListItem
import top.kagg886.pmf.ui.route.main.search.v2.toDisplayString
import top.kagg886.pmf.util.stringResource

sealed interface TagPropertiesState {
    data object Loading : TagPropertiesState
    data class Loaded(val tags: List<TrendingTags>) : TagPropertiesState
    data class Failed(val msg: String) : TagPropertiesState
}

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
                Text(stringResource(Res.string.sort_mode))
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
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            },
        )
        ListItem(
            headlineContent = {
                Text(stringResource(Res.string.search_mode))
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
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            },
        )
        SupportListItem(
            trailingContent = {
                IconButton(
                    onClick = { onTagRequestRefresh() },
                    enabled = tag != TagPropertiesState.Loading,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        null,
                    )
                }
            },
            headlineContent = {
                Text(stringResource(Res.string.hot_tags))
            },
            supportingContent = {
                when (tag) {
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
                                    modifier = Modifier.padding(4.dp),
                                )
                            }
                        }
                    }
                    is TagPropertiesState.Failed -> Text(stringResource(Res.string.load_failed))
                }
            },
        )
    }
}
