package com.rizilab.averroes.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base MVI ViewModel
 */
abstract class MviViewModel<STATE : Any, INTENT : Any, EFFECT : Any>(
    initialState: STATE
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<STATE> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<EFFECT>()
    val effect: SharedFlow<EFFECT> = _effect.asSharedFlow()

    protected fun updateState(newState: STATE) {
        _state.value = newState
    }

    protected fun updateState(reducer: STATE.() -> STATE) {
        _state.value = _state.value.reducer()
    }

    protected fun sendEffect(effect: EFFECT) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }

    abstract fun handleIntent(intent: INTENT)
}

/**
 * Common loading state
 */
data class LoadingState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val error: String? = null
) {
    val hasData: Boolean get() = data != null
    val hasError: Boolean get() = error != null
    
    companion object {
        fun <T> loading(): LoadingState<T> = LoadingState(isLoading = true)
        fun <T> success(data: T): LoadingState<T> = LoadingState(data = data)
        fun <T> error(message: String): LoadingState<T> = LoadingState(error = message)
    }
}
