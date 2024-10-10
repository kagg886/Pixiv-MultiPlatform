package top.kagg886.pmf.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Loading(modifier: Modifier = Modifier, text: String? = "") {
    Box(modifier=modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            text?.let {
                Text(it)
            }
        }
    }
}