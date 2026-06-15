package com.winschneid.myliverecord.ui.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.usecase.AddLiveRecordUseCase
import com.winschneid.myliverecord.domain.usecase.DeleteLiveRecordUseCase
import com.winschneid.myliverecord.domain.usecase.GetArtistNamesUseCase
import com.winschneid.myliverecord.domain.usecase.GetLiveRecordByIdUseCase
import com.winschneid.myliverecord.domain.usecase.GetLiveRecordsUseCase
import com.winschneid.myliverecord.domain.usecase.GetVenueNamesUseCase
import com.winschneid.myliverecord.domain.usecase.UpdateLiveRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddLiveUiState(
    val title: String = "",
    val artistNames: List<String> = listOf(""),
    val venueName: String = "",
    val seatNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val memo: String = "",
    val ticketPriceText: String = "",
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val allArtistNames: List<String> = emptyList(), // サジェスト用（フィルタはUI側で実施）
    val venueSuggestions: List<String> = emptyList(),
    val duplicateWarning: String? = null, // 同日の既存記録がある場合の確認メッセージ
)

sealed interface AddLiveAction {
    data class UpdateTitle(val value: String) : AddLiveAction
    data class UpdateArtistName(val index: Int, val value: String) : AddLiveAction
    data object AddArtist : AddLiveAction
    data class RemoveArtist(val index: Int) : AddLiveAction
    data class UpdateVenueName(val value: String) : AddLiveAction
    data class UpdateSeatNumber(val value: String) : AddLiveAction
    data class UpdateDate(val value: Long) : AddLiveAction
    data class UpdateMemo(val value: String) : AddLiveAction
    data class UpdateTicketPrice(val value: String) : AddLiveAction
    data object Save : AddLiveAction
    data object ConfirmSave : AddLiveAction
    data object DismissDuplicateWarning : AddLiveAction
    data object Delete : AddLiveAction
}

private data class InputState(
    val title: String = "",
    val artistNames: List<String> = listOf(""),
    val venueName: String = "",
    val seatNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val memo: String = "",
    val ticketPriceText: String = "",
    val isSaved: Boolean = false,
    val duplicateWarning: String? = null,
)

@HiltViewModel
class AddLiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addLiveRecord: AddLiveRecordUseCase,
    private val updateLiveRecord: UpdateLiveRecordUseCase,
    private val deleteLiveRecord: DeleteLiveRecordUseCase,
    private val getLiveRecordById: GetLiveRecordByIdUseCase,
    private val getLiveRecords: GetLiveRecordsUseCase,
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
            title = input.title,
            artistNames = input.artistNames,
            venueName = input.venueName,
            seatNumber = input.seatNumber,
            date = input.date,
            memo = input.memo,
            ticketPriceText = input.ticketPriceText,
            isSaved = input.isSaved,
            duplicateWarning = input.duplicateWarning,
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
                        title = record.title,
                        artistNames = record.artistNames.ifEmpty { listOf("") },
                        venueName = record.venueName,
                        seatNumber = record.seatNumber,
                        date = record.date,
                        memo = record.memo,
                        ticketPriceText = record.ticketPrice?.toString() ?: "",
                    )
                }
            }
        }
    }

    fun onAction(action: AddLiveAction) {
        when (action) {
            is AddLiveAction.UpdateTitle -> _input.update { it.copy(title = action.value) }
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
            is AddLiveAction.UpdateMemo -> _input.update { it.copy(memo = action.value) }
            is AddLiveAction.UpdateTicketPrice -> _input.update {
                it.copy(ticketPriceText = action.value.filter { c -> c.isDigit() })
            }
            AddLiveAction.Save -> save(force = false)
            AddLiveAction.ConfirmSave -> save(force = true)
            AddLiveAction.DismissDuplicateWarning -> _input.update { it.copy(duplicateWarning = null) }
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

    private fun save(force: Boolean) {
        val input = _input.value
        val validArtists = input.artistNames.map { it.trim() }.filter { it.isNotEmpty() }
        if (validArtists.isEmpty() || input.venueName.isBlank()) return
        viewModelScope.launch {
            if (!force) {
                val duplicate = findSameDayRecord(input.date)
                if (duplicate != null) {
                    _input.update {
                        it.copy(
                            duplicateWarning = "同じ日に「${duplicate.artistNames.joinToString("、")}」" +
                                "（${duplicate.venueName}）が登録されています。このまま保存しますか？",
                        )
                    }
                    return@launch
                }
            }
            val record = LiveRecord(
                id = recordId ?: 0L,
                title = input.title.trim(),
                artistNames = validArtists,
                venueName = input.venueName.trim(),
                seatNumber = input.seatNumber.trim(),
                date = input.date,
                memo = input.memo.trim(),
                ticketPrice = input.ticketPriceText.toLongOrNull(),
            )
            if (recordId != null) updateLiveRecord(record) else addLiveRecord(record)
            _input.update { it.copy(duplicateWarning = null, isSaved = true) }
        }
    }

    /** 編集中の記録自身を除き、同じ日（ローカルタイムゾーン基準）の既存記録を返す */
    private suspend fun findSameDayRecord(date: Long): LiveRecord? {
        val target = Calendar.getInstance().apply { timeInMillis = date }
        return getLiveRecords().first().firstOrNull { existing ->
            if (existing.id == recordId) return@firstOrNull false
            val cal = Calendar.getInstance().apply { timeInMillis = existing.date }
            cal.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        }
    }
}
