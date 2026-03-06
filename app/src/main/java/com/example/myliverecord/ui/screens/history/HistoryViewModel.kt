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
    val artistName: String,
    val venueName: String,
    val seatNumber: String,
    val date: Long,
    val artistVisitCount: Int,
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
            val artistCounts = records.groupingBy { it.artistName }.eachCount()
            val items = records.map { record ->
                LiveRecordItem(
                    id = record.id,
                    artistName = record.artistName,
                    venueName = record.venueName,
                    seatNumber = record.seatNumber,
                    date = record.date,
                    artistVisitCount = artistCounts[record.artistName] ?: 1,
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
