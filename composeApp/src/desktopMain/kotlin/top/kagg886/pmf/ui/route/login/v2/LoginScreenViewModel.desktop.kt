@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.pmf.ui.route.login.v2

import kotlinx.coroutines.Job
import okio.Path

@Suppress("DefaultLocale")
actual fun LoginScreenViewModel.initKCEF() = intent {
}

actual fun LoginScreenViewModel.initKCEFLocal(file: Path): Job = intent {
}
