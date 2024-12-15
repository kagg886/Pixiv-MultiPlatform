package top.kagg886.pmf

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.room.Room
import androidx.room.RoomDatabase
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.kagg886.pmf.backend.database.AppDatabase
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import kotlin.reflect.full.primaryConstructor

@Composable
actual fun AppScaffold(nav: Navigator, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = LocalSnackBarHost.current)
        }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(it)) {
            if (NavigationItem.entries.any { item -> item.screenClass.isInstance(nav.lastItemOrNull) }) {
                NavigationRail {
                    SearchButton()
                    for (entry in NavigationItem.entries) {
                        NavigationRailItem(
                            selected = entry.screenClass.isInstance(nav.lastItemOrNull),
                            onClick = {
                                if (entry.screenClass.isInstance(nav.lastItemOrNull)) {
                                    return@NavigationRailItem
                                }
                                nav.push(entry.screenClass.primaryConstructor!!.call())
                            },
                            icon = {
                                Icon(imageVector = entry.icon, null)
                            },
                            label = {
                                Text(entry.title)
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    ProfileAvatar()
                }
            }
            content(Modifier.fillMaxSize())
        }

    }
}

actual fun shareFile(file: File) {
    Desktop.getDesktop().open(file)
}