package com.mobilemoney.ui.common

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

object ErrorHandler {
    private val _errorChannel = Channel<String>(Channel.BUFFERED)

    val errorFlow: Flow<String> = _errorChannel.receiveAsFlow()

    fun showError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    suspend fun emitError(message: String) {
        _errorChannel.send(message)
    }
}