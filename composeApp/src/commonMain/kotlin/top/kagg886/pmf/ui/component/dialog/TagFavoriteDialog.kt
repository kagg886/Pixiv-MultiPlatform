package top.kagg886.pmf.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.BookmarkVisibility
import top.kagg886.pmf.res.*
import top.kagg886.pmf.util.stringResource

@Composable
fun TagFavoriteDialog(
    tags: List<Tag>,
    title: @Composable () -> Unit,
    confirm: suspend (List<Tag>, BookmarkVisibility) -> Unit,
    cancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val selectTags = remember {
        mutableStateListOf<Tag>()
    }
    var restrict by remember {
        mutableStateOf(BookmarkVisibility.PUBLIC)
    }
    AlertDialog(
        onDismissRequest = cancel,
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        confirm(selectTags, restrict)
                    }
                },
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = cancel) {
                Text(stringResource(Res.string.cancel))
            }
        },
        title = title,
        text = {
            Column {
                ListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.favorite_type))
                    },
                    supportingContent = {
                        PrimaryTabRow(
                            selectedTabIndex = restrict.ordinal,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            for (i in BookmarkVisibility.entries) {
                                Tab(
                                    selected = restrict == i,
                                    onClick = {
                                        restrict = i
                                    },
                                    text = {
                                        Text(text = i.name)
                                    },
                                )
                            }
                        }
                    },
                )
                ListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.favorite_tag))
                    },
                    supportingContent = {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (tag in tags) {
                                FilterChip(
                                    selected = selectTags.contains(tag),
                                    label = {
                                        Text(text = tag.name)
                                    },
                                    onClick = {
                                        if (selectTags.contains(tag)) {
                                            selectTags.remove(tag)
                                        } else {
                                            selectTags.add(tag)
                                        }
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
    )
}
