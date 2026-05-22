package com.mobilemoney.domain.usecase.clipboard

import com.mobilemoney.domain.repository.MessageRegexRepository
import java.util.UUID

class DeleteMessageRegexUseCase(
    private val repository: MessageRegexRepository
) {
    suspend operator fun invoke(id: UUID) {
        repository.deleteRegex(id)
    }
}
