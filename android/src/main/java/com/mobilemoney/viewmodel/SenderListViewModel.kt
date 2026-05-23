package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.data.local.SenderEntity
import com.mobilemoney.di.DI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class SenderListViewModel : ViewModel() {
    val senders: Flow<List<SenderEntity>> = DI.databaseRepository.getSenders()

    fun addSender(senderNumber: String, label: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = SenderEntity(
                id = UUID.randomUUID().toString(),
                sender = senderNumber,
                label = label?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now
            )
            DI.databaseRepository.addSender(entity)
        }
    }

    fun deleteSender(id: String) {
        viewModelScope.launch {
            DI.databaseRepository.deleteSender(id)
        }
    }
}
