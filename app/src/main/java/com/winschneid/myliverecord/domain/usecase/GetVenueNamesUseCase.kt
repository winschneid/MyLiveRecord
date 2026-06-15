package com.winschneid.myliverecord.domain.usecase

import com.winschneid.myliverecord.domain.repository.LiveRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVenueNamesUseCase @Inject constructor(
    private val repository: LiveRecordRepository,
) {
    operator fun invoke(): Flow<List<String>> = repository.observeDistinctVenueNames()
}
