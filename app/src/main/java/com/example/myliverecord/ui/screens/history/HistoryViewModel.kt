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
            // 日付昇順で処理し、そのレコード時点での「n回目」を計算する
            val artistRunningCounts = mutableMapOf<String, Int>()
            val visitCountsById = mutableMapOf<Long, Map<String, Int>>()
            records.sortedBy { it.date }.forEach { record ->
                val visitCounts = record.artistNames.associateWith { artistName ->
                    val count = (artistRunningCounts[artistName] ?: 0) + 1
                    artistRunningCounts[artistName] = count
                    count
                }
                visitCountsById[record.id] = visitCounts
            }
            // 表示順（日付降順）は DAO の ORDER BY date DESC を維持
            val items = records.map { record ->
                LiveRecordItem(
                    id = record.id,
                    artistNames = record.artistNames,
                    venueName = record.venueName,
                    seatNumber = record.seatNumber,
                    date = record.date,
                    artistVisitCounts = visitCountsById[record.id] ?: emptyMap(),
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
