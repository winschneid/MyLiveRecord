package com.example.myliverecord.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myliverecord.domain.model.YearSummary
import com.example.myliverecord.domain.usecase.GetYearSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class YearSummaryUiState(
    val years: List<YearSummary> = emptyList(),
    val isLoading: Boolean = true,
    val expandedYears: Set<Int> = emptySet(),
)

sealed interface YearSummaryAction {
    data class ToggleYear(val year: Int) : YearSummaryAction
}

@HiltViewModel
class YearSummaryViewModel @Inject constructor(
    getYearSummary: GetYearSummaryUseCase,
) : ViewModel() {

    private val _expandedYears = MutableStateFlow<Set<Int>>(emptySet())

    val uiState = combine(
        getYearSummary(),
        _expandedYears,
    ) { years, expandedYears ->
        YearSummaryUiState(
            years = years,
            isLoading = false,
            expandedYears = expandedYears,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = YearSummaryUiState(),
    )

    fun onAction(action: YearSummaryAction) {
        when (action) {
            is YearSummaryAction.ToggleYear -> _expandedYears.update { current ->
                if (action.year in current) current - action.year else current + action.year
            }
        }
    }
}
