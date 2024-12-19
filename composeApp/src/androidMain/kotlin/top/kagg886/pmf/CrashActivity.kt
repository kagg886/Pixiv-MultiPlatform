package top.kagg886.pmf

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import top.kagg886.pmf.ui.route.crash.CrashApp

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("exceptions", Throwable::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("exceptions") as Throwable
        }
        setContent {
            CrashApp(throwable = ex)
        }
    }
}