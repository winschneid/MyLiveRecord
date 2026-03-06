package com.example.myliverecord.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myliverecord.domain.model.LiveRecord
import com.example.myliverecord.domain.usecase.AddLiveRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddLiveUiState(
    val artistName: String = "",
    val venueName: String = "",
    val seatNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
)

sealed interface AddLiveAction {
    data class UpdateArtistName(val value: String) : AddLiveAction
    data class UpdateVenueName(val value: String) : AddLiveAction
    data class UpdateSeatNumber(val value: String) : AddLiveAction
    data class UpdateDate(val value: Long) : AddLiveAction
    data object Save : AddLiveAction
}

@HiltViewModel
class AddLiveViewModel @Inject constructor(
    private val addLiveRecord: AddLiveRecordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddLiveUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: AddLiveAction) {
        when (action) {
            is AddLiveAction.UpdateArtistName -> _uiState.update { it.copy(artistName = action.value) }
            is AddLiveAction.UpdateVenueName -> _uiState.update { it.copy(venueName = action.value) }
            is AddLiveAction.UpdateSeatNumber -> _uiState.update { it.copy(seatNumber = action.value) }
            is AddLiveAction.UpdateDate -> _uiState.update { it.copy(date = action.value) }
            AddLiveAction.Save -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.artistName.isBlank() || state.venueName.isBlank()) return
        viewModelScope.launch {
            addLiveRecord(
                LiveRecord(
                    artistName = state.artistName.trim(),
                    venueName = state.venueName.trim(),
                    seatNumber = state.seatNumber.trim(),
                    date = state.date,
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
