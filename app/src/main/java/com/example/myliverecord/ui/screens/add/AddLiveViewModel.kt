package com.example.myliverecord.ui.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myliverecord.domain.model.LiveRecord
import com.example.myliverecord.domain.usecase.AddLiveRecordUseCase
import com.example.myliverecord.domain.usecase.DeleteLiveRecordUseCase
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
    val artistNames: List<String> = listOf(""),
    val venueName: String = "",
    val seatNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val allArtistNames: List<String> = emptyList(), // サジェスト用（フィルタはUI側で実施）
    val venueSuggestions: List<String> = emptyList(),
)

sealed interface AddLiveAction {
    data class UpdateArtistName(val index: Int, val value: String) : AddLiveAction
    data object AddArtist : AddLiveAction
    data class RemoveArtist(val index: Int) : AddLiveAction
    data class UpdateVenueName(val value: String) : AddLiveAction
    data class UpdateSeatNumber(val value: String) : AddLiveAction
    data class UpdateDate(val value: Long) : AddLiveAction
    data object Save : AddLiveAction
    data object Delete : AddLiveAction
}

private data class InputState(
    val artistNames: List<String> = listOf(""),
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
    private val deleteLiveRecord: DeleteLiveRecordUseCase,
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
            artistNames = input.artistNames,
            venueName = input.venueName,
            seatNumber = input.seatNumber,
            date = input.date,
            isSaved = input.isSaved,
            isEditMode = recordId != null,
            allArtistNames = artistNames,
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
                        artistNames = record.artistNames.ifEmpty { listOf("") },
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
            is AddLiveAction.UpdateArtistName -> _input.update {
                it.copy(artistNames = it.artistNames.toMutableList().also { list ->
                    list[action.index] = action.value
                })
            }
            AddLiveAction.AddArtist -> _input.update {
                it.copy(artistNames = it.artistNames + "")
            }
            is AddLiveAction.RemoveArtist -> _input.update {
                it.copy(artistNames = it.artistNames.toMutableList().also { list ->
                    list.removeAt(action.index)
                })
            }
            is AddLiveAction.UpdateVenueName -> _input.update { it.copy(venueName = action.value) }
            is AddLiveAction.UpdateSeatNumber -> _input.update { it.copy(seatNumber = action.value) }
            is AddLiveAction.UpdateDate -> _input.update { it.copy(date = action.value) }
            AddLiveAction.Save -> save()
            AddLiveAction.Delete -> delete()
        }
    }

    private fun delete() {
        if (recordId == null) return
        viewModelScope.launch {
            deleteLiveRecord(recordId)
            _input.update { it.copy(isSaved = true) }
        }
    }

    private fun save() {
        val input = _input.value
        val validArtists = input.artistNames.map { it.trim() }.filter { it.isNotEmpty() }
        if (validArtists.isEmpty() || input.venueName.isBlank()) return
        viewModelScope.launch {
            val record = LiveRecord(
                id = recordId ?: 0L,
                artistNames = validArtists,
                venueName = input.venueName.trim(),
                seatNumber = input.seatNumber.trim(),
                date = input.date,
            )
            if (recordId != null) updateLiveRecord(record) else addLiveRecord(record)
            _input.update { it.copy(isSaved = true) }
        }
    }
}
