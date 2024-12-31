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

        //防止Suppress跨Activity传输导致的崩溃问题
        val sameExceptionObject = ex::class.constructors.first {
            //ex::class.constructors.toList()[2].parameters[1].type.classifier!! == Throwable::class
            if (it.parameters.size != 2) {
                return@first false
            }
            val argA = it.parameters[0].type.classifier!!
            val argB = it.parameters[1].type.classifier!!

            argA == String::class && argB == Throwable::class
        }.call(
            ex.message,
            ex.cause
        )


        setContent {
            CrashApp(throwable = sameExceptionObject)
        }
    }
}