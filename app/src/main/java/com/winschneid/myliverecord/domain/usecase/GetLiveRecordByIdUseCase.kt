package com.winschneid.myliverecord.domain.usecase

import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.repository.LiveRecordRepository
import javax.inject.Inject

class GetLiveRecordByIdUseCase @Inject constructor(
    private val repository: LiveRecordRepository,
) {
    suspend operator fun invoke(id: Long): LiveRecord? = repository.getRecordById(id)
}
