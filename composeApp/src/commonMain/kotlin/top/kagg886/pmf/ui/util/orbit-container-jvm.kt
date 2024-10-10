package top.kagg886.pmf.ui.util

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.SettingsBuilder
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

fun <STATE : Any, SIDE_EFFECT : Any> ViewModel.container(
    initialState: STATE,
    buildSettings: SettingsBuilder.() -> Unit = {},
    onCreate: (suspend Syntax<STATE, SIDE_EFFECT>.() -> Unit)? = null
): Container<STATE, SIDE_EFFECT> {
    return viewModelScope.container(initialState, buildSettings, onCreate)
}

@Composable
fun <STATE : Any, SIDE_EFFECT : Any> ContainerHost<STATE, SIDE_EFFECT>.collectSideEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    sideEffect: (suspend (sideEffect: SIDE_EFFECT) -> Unit)
) {
    val sideEffectFlow = container.refCountSideEffectFlow
    val lifecycleOwner = LocalLifecycleOwner.current

    val callback by rememberUpdatedState(newValue = sideEffect)

    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            sideEffectFlow.collect { callback(it) }
        }
    }
}

@Composable
fun <STATE : Any, SIDE_EFFECT : Any> ContainerHost<STATE, SIDE_EFFECT>.collectAsState(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED
): State<STATE> {
    return container.refCountStateFlow.collectAsStateWithLifecycle(minActiveState = lifecycleState)
}