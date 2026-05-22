package com.mobilemoney.domain.usecase.clipboard

import com.mobilemoney.domain.model.MessageRegex
import com.mobilemoney.domain.repository.MessageRegexRepository
import kotlinx.coroutines.flow.Flow

class GetMessageRegexesUseCase(
    private val repository: MessageRegexRepository
) {
    operator fun invoke(): Flow<List<MessageRegex>> {
        return repository.getRegexes()
    }
}
