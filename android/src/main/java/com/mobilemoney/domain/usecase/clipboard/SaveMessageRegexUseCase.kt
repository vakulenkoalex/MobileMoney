package com.mobilemoney.domain.usecase.clipboard

import com.mobilemoney.domain.model.MessageRegex
import com.mobilemoney.domain.repository.MessageRegexRepository

class SaveMessageRegexUseCase(
    private val repository: MessageRegexRepository
) {
    suspend operator fun invoke(regex: MessageRegex) {
        repository.saveRegex(regex)
    }
}
