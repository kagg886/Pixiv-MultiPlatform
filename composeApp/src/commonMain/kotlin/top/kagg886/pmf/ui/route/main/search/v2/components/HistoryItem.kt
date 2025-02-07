package top.kagg886.pmf.ui.route.main.search.v2.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.kagg886.pixko.module.search.SearchSort.*
import top.kagg886.pixko.module.search.SearchTarget.*
import top.kagg886.pmf.backend.database.dao.SearchHistory
import top.kagg886.pmf.ui.route.main.search.v2.toDisplayString

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryItem(
    modifier: Modifier = Modifier,
    onHistoryDelete: () -> Unit,
    onHistoryClicked: () -> Unit,
    item: SearchHistory
) {
    ListItem(
        modifier = modifier.clickable(onClick = onHistoryClicked),
        overlineContent = {
            Text(item.keyword.joinToString())
        },
        trailingContent = {
            IconButton(onClick = onHistoryDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "")
            }
        },
        headlineContent = {
            FlowRow {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(item.initialSort.toDisplayString())
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(item.initialTarget.toDisplayString())
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
    )

}