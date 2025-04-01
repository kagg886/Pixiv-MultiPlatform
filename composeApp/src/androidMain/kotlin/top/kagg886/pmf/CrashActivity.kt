package top.kagg886.pmf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import top.kagg886.pmf.ui.route.crash.CrashApp

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ex = intent.getStringExtra("exceptions")!!
        setContent {
            CrashApp(throwable = ex)
        }
    }
}
