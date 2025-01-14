package top.kagg886.pmf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEvent
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val flow = MutableSharedFlow<KeyEvent>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FileKit.init(this)
        setContent {
            CompositionLocalProvider(
                LocalKeyStateFlow provides flow
            ) {
                App()
            }
        }
    }
}