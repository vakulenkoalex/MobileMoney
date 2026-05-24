package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.data.local.MessageEntity
import com.mobilemoney.di.DI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MessageListViewModel : ViewModel() {
    val messages: Flow<List<MessageEntity>> = DI.databaseRepository.getMessages()

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            DI.databaseRepository.deleteMessageById(id)
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            DI.databaseRepository.deleteAllMessages()
        }
    }
}
