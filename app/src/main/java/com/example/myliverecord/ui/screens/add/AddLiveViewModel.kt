package com.example.myliverecord.ui.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myliverecord.domain.model.LiveRecord
import com.example.myliverecord.domain.usecase.AddLiveRecordUseCase
import com.example.myliverecord.domain.usecase.GetArtistNamesUseCase
import com.example.myliverecord.domain.usecase.GetLiveRecordByIdUseCase
import com.example.myliverecord.domain.usecase.GetVenueNamesUseCase
import com.example.myliverecord.domain.usecase.UpdateLiveRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddLiveUiState(
    val artistName: String = "",
    val venueName: String = "",
    val seatNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val artistSuggestions: List<String> = emptyList(),
    val venueSuggestions: List<String> = emptyList(),
)

sealed interface AddLiveAction {
    data class UpdateArtistName(val value: String) : AddLiveAction
    data class UpdateVenueName(val value: String) : AddLiveAction
    data class UpdateSeatNumber(val value: String) : AddLiveAction
    data class UpdateDate(val value: Long) : AddLiveAction
    data object Save : AddLiveAction
}

private data class InputState(
    val artistName: String = "",
    val venueName: String = "",
    val seatNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
)

@HiltViewModel
class AddLiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addLiveRecord: AddLiveRecordUseCase,
    private val updateLiveRecord: UpdateLiveRecordUseCase,
    private val getLiveRecordById: GetLiveRecordByIdUseCase,
    getArtistNames: GetArtistNamesUseCase,
    getVenueNames: GetVenueNamesUseCase,
) : ViewModel() {

    private val recordId: Long? = savedStateHandle.get<Long>("recordId")

    private val _input = MutableStateFlow(InputState())

    val uiState = combine(
        _input,
        getArtistNames(),
        getVenueNames(),
    ) { input, artistNames, venueNames ->
        AddLiveUiState(
            artistName = input.artistName,
            venueName = input.venueName,
            seatNumber = input.seatNumber,
            date = input.date,
            isSaved = input.isSaved,
            isEditMode = recordId != null,
            artistSuggestions = if (input.artistName.isBlank()) emptyList()
            else artistNames.filter {
                it.contains(input.artistName, ignoreCase = true) && !it.equals(input.artistName, ignoreCase = true)
            },
            venueSuggestions = if (input.venueName.isBlank()) emptyList()
            else venueNames.filter {
                it.contains(input.venueName, ignoreCase = true) && !it.equals(input.venueName, ignoreCase = true)
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddLiveUiState(isEditMode = recordId != null),
    )

    init {
        if (recordId != null) {
            viewModelScope.launch {
                getLiveRecordById(recordId)?.let { record ->
                    _input.value = InputState(
                        artistName = record.artistName,
                        venueName = record.venueName,
                        seatNumber = record.seatNumber,
                        date = record.date,
                    )
                }
            }
        }
    }

    fun onAction(action: AddLiveAction) {
        when (action) {
            is AddLiveAction.UpdateArtistName -> _input.update { it.copy(artistName = action.value) }
            is AddLiveAction.UpdateVenueName -> _input.update { it.copy(venueName = action.value) }
            is AddLiveAction.UpdateSeatNumber -> _input.update { it.copy(seatNumber = action.value) }
            is AddLiveAction.UpdateDate -> _input.update { it.copy(date = action.value) }
            AddLiveAction.Save -> save()
        }
    }

    private fun save() {
        val input = _input.value
        if (input.artistName.isBlank() || input.venueName.isBlank()) return
        viewModelScope.launch {
            val record = LiveRecord(
                id = recordId ?: 0L,
                artistName = input.artistName.trim(),
                venueName = input.venueName.trim(),
                seatNumber = input.seatNumber.trim(),
                date = input.date,
            )
            if (recordId != null) updateLiveRecord(record) else addLiveRecord(record)
            _input.update { it.copy(isSaved = true) }
        }
    }
}
