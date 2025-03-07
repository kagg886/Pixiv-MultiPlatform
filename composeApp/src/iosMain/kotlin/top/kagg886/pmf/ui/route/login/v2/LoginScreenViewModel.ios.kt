package top.kagg886.pmf.ui.route.login.v2

import kotlinx.coroutines.Job
import okio.Path

actual fun LoginScreenViewModel.initKCEF(): Job {
    error("IOS can't init KCEF!")
}

actual fun LoginScreenViewModel.initKCEFLocal(file: Path): Job {
    throw UnsupportedOperationException()
}
