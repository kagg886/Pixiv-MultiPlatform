package top.kagg886.pmf

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import top.kagg886.filepicker.FilePicker

class MainActivity : ComponentActivity() {
    private val flow = MutableSharedFlow<KeyEvent>()
    private val scope = CoroutineScope(Dispatchers.Main)

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        scope.launch {
            flow.emit(KeyEvent(event))
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FilePicker.init(this)
        setContent {
            CompositionLocalProvider(
                LocalKeyStateFlow provides flow,
            ) {
                App()
            }
        }
    }
}
