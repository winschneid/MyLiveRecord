package com.example.myliverecord.domain.usecase

import com.example.myliverecord.domain.model.LiveRecord
import com.example.myliverecord.domain.repository.LiveRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLiveRecordsUseCase @Inject constructor(
    private val repository: LiveRecordRepository
) {
    operator fun invoke(): Flow<List<LiveRecord>> = repository.observeAllRecords()
}
