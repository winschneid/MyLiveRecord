package com.example.myliverecord.domain.usecase

import com.example.myliverecord.domain.repository.LiveRecordRepository
import javax.inject.Inject

class DeleteLiveRecordUseCase @Inject constructor(
    private val repository: LiveRecordRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteRecord(id)
}
