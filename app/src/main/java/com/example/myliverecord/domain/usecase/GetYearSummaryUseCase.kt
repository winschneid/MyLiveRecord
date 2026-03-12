package com.example.myliverecord.domain.usecase

import com.example.myliverecord.domain.model.ArtistCount
import com.example.myliverecord.domain.model.YearSummary
import com.example.myliverecord.domain.repository.LiveRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class GetYearSummaryUseCase @Inject constructor(
    private val repository: LiveRecordRepository,
) {
    operator fun invoke(): Flow<List<YearSummary>> =
        repository.observeAllRecords().map { records ->
            records
                .groupBy { record ->
                    Calendar.getInstance().apply { timeInMillis = record.date }.get(Calendar.YEAR)
                }
                .map { (year, yearRecords) ->
                    YearSummary(
                        year = year,
                        totalCount = yearRecords.size,
                        artists = yearRecords
                            .flatMap { it.artistNames }
                            .groupingBy { it }
                            .eachCount()
                            .entries
                            .sortedByDescending { it.value }
                            .map { ArtistCount(it.key, it.value) },
                    )
                }
                .sortedByDescending { it.year }
        }
}
