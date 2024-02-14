package com.example.projektmunka.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel() : ViewModel() {

    val error = MutableSharedFlow<String>()

    val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        throwable.printStackTrace()
        val message = throwable.message
        viewModelScope.launch() {
            if (message == null) {
                error.emit("Unknown error")
            } else
                error.emit(message)
        }
    }
    val coroutineContext = coroutineExceptionHandler+Dispatchers.IO
}