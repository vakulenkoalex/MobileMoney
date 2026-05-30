package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.Sender
import com.mobilemoney.domain.model.SenderType
import com.mobilemoney.domain.repository.SenderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SenderListViewModel(
    private val senderRepository: SenderRepository
) : ViewModel() {
    val senders: Flow<List<Sender>> = senderRepository.getSenders()

    fun addSender(senderNumber: String, label: String) {
        viewModelScope.launch {
            senderRepository.addSender(senderNumber, label, SenderType.PHONE_NUMBER)
        }
    }

    fun deleteSender(id: String) {
        viewModelScope.launch {
            senderRepository.deleteSender(id)
        }
    }
}
