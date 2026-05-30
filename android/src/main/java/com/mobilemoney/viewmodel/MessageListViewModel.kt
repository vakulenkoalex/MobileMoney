package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.Message
import com.mobilemoney.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MessageListViewModel(
    private val messageRepository: MessageRepository
) : ViewModel() {
    val messages: Flow<List<Message>> = messageRepository.getMessages()

    fun markAsProcessed(id: String) {
        viewModelScope.launch {
            messageRepository.markProcessed(id)
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            messageRepository.deleteById(id)
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            messageRepository.deleteAll()
        }
    }
}
