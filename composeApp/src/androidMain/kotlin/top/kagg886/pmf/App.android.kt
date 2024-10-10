package top.kagg886.pmf

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.room.RoomDatabase
import cafe.adriel.voyager.navigator.Navigator
import top.kagg886.pmf.backend.database.AppDatabase
import java.io.File
import kotlin.reflect.full.primaryConstructor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun AppScaffold(nav: Navigator, content: @Composable (Modifier) -> Unit) {
    var title by remember {
        mutableStateOf("推荐")
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                LocalSnackBarHost.current
            )
        },
        topBar = {
            if (NavigationItem.entries.any { it.screenClass.isInstance(nav.lastItemOrNull) }) {
                TopAppBar(
                    title = {
                        Text(title)
                    },
                    navigationIcon = {
                        ProfileAvatar()
                    },
                    actions = {
                        SearchButton()
                    }
                )
            }
        },
        bottomBar = {
            if (NavigationItem.entries.any { it.screenClass.isInstance(nav.lastItemOrNull) }) {
                NavigationBar {
                    for (entry in NavigationItem.entries) {
                        NavigationBarItem(
                            selected = entry.screenClass.isInstance(nav.lastItemOrNull),
                            onClick = {
                                if (entry.screenClass.isInstance(nav.lastItemOrNull)) {
                                    return@NavigationBarItem
                                }
                                title = entry.title
                                nav.push(entry.screenClass.primaryConstructor!!.call())
                            },
                            icon = {
                                Icon(imageVector = entry.icon,null)
                            },
                            label = {
                                Text(entry.title)
                            }
                        )
                    }
                }

            }
        }
    ) {
        content(Modifier.padding(it))
    }
}

actual fun getDataBaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        name = databasePath.absolutePath,
        context = PMFApplication.getApp()
    )
}

actual fun shareFile(file: File) {
    with(PMFApplication.getApp()) {
        val intent = Intent("android.intent.action.SEND")
        intent.putExtra(
            "android.intent.extra.STREAM",
            FileProvider.getUriForFile(
                this,
                "${BuildConfig.APP_BASE_PACKAGE}.fileprovider",
                file
            )
        )
        intent.flags = FLAG_ACTIVITY_NEW_TASK
        intent.setType("*/*")
        ContextCompat.startActivity(this,intent,null)
    }
}