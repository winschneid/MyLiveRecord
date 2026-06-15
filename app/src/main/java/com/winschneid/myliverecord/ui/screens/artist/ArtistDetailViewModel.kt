package com.winschneid.myliverecord.ui.screens.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winschneid.myliverecord.domain.usecase.GetLiveRecordsUseCase
import com.winschneid.myliverecord.ui.screens.history.computeVisitCounts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ArtistLiveItem(
    val id: Long,
    val title: String,
    val venueName: String,
    val seatNumber: String,
    val date: Long,
    val visitNumber: Int, // そのライブ時点でのn回目
)

data class ArtistDetailUiState(
    val artistName: String = "",
    val totalCount: Int = 0,
    val firstDate: Long? = null,
    val items: List<ArtistLiveItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getLiveRecords: GetLiveRecordsUseCase,
) : ViewModel() {

    private val artistName: String = savedStateHandle.get<String>("artistName").orEmpty()

    val uiState = getLiveRecords()
        .map { records ->
            val visitCountsById = computeVisitCounts(records)
            // 表示順（日付降順）は DAO の ORDER BY date DESC を維持
            val items = records
                .filter { artistName in it.artistNames }
                .map { record ->
                    ArtistLiveItem(
                        id = record.id,
                        title = record.title,
                        venueName = record.venueName,
                        seatNumber = record.seatNumber,
                        date = record.date,
                        visitNumber = visitCountsById[record.id]?.get(artistName) ?: 1,
                    )
                }
            ArtistDetailUiState(
                artistName = artistName,
                totalCount = items.size,
                firstDate = items.minOfOrNull { it.date },
                items = items,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArtistDetailUiState(artistName = artistName),
        )
}
