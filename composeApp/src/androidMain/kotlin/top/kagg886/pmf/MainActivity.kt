package top.kagg886.pmf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEvent
import com.gyf.immersionbar.ImmersionBar
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private val flow = MutableSharedFlow<KeyEvent>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        setContent {
            CompositionLocalProvider(
                LocalKeyStateFlow provides flow
            ) {
                App(
                    onNightModeListener = {
                        val bar = ImmersionBar.with(this).transparentBar()
                        bar.statusBarDarkFont(!it)
                        bar.navigationBarDarkIcon(!it)
                        bar.init()
                    }
                )
            }
        }
    }
}