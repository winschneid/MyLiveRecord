package com.example.myliverecord.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myliverecord.domain.usecase.GetLiveRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LiveRecordItem(
    val id: Long,
    val artistNames: List<String>,
    val venueName: String,
    val seatNumber: String,
    val date: Long,
    val artistVisitCounts: Map<String, Int>, // アーティスト名 → 累計回数
)

data class HistoryUiState(
    val records: List<LiveRecordItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getLiveRecords: GetLiveRecordsUseCase,
) : ViewModel() {

    val uiState = getLiveRecords()
        .map { records ->
            // 各アーティストが何回登場するかを集計（複数アーティスト記録も含む）
            val artistCounts = records
                .flatMap { it.artistNames }
                .groupingBy { it }
                .eachCount()
            val items = records.map { record ->
                LiveRecordItem(
                    id = record.id,
                    artistNames = record.artistNames,
                    venueName = record.venueName,
                    seatNumber = record.seatNumber,
                    date = record.date,
                    artistVisitCounts = record.artistNames.associateWith { artistCounts[it] ?: 1 },
                )
            }
            HistoryUiState(records = items, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )
}
