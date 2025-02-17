package top.kagg886.pmf.ui.route.login.v2

import kotlinx.coroutines.Job

actual fun LoginScreenViewModel.initKCEF(): Job {
    error("IOS can't init KCEF!")
}