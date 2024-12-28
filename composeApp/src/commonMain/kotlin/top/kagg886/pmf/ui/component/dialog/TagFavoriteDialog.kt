package top.kagg886.pmf.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.BookmarkVisibility

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagFavoriteDialog(
    tags: List<Tag>,
    title:@Composable () -> Unit,
    confirm: suspend (List<Tag>,BookmarkVisibility) -> Unit,
    cancel: () -> Unit
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
                        confirm(selectTags,restrict)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = cancel) {
                Text("取消")
            }
        },
        title = title,
        text = {
            Column {
                ListItem(
                    headlineContent = {
                        Text("收藏类型(必填)")
                    },
                    supportingContent = {
                        PrimaryTabRow(
                            selectedTabIndex = restrict.ordinal,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (i in BookmarkVisibility.entries) {
                                Tab(
                                    selected = restrict == i,
                                    onClick = {
                                        restrict = i
                                    },
                                    text = {
                                        Text(text = i.name)
                                    }
                                )
                            }
                        }
                    }
                )
                ListItem(
                    headlineContent = {
                        Text("标签收藏(可不填写)")
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
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    )
}