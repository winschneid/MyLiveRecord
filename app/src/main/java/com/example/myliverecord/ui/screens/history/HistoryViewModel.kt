package com.example.myliverecord.ui.screens.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myliverecord.data.transfer.LiveRecordsJson
import com.example.myliverecord.domain.model.LiveRecord
import com.example.myliverecord.domain.usecase.AddLiveRecordUseCase
import com.example.myliverecord.domain.usecase.GetLiveRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LiveRecordItem(
    val id: Long,
    val title: String = "",
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

/**
 * 日付昇順で処理し、各レコード時点でアーティストごとの「n回目」を計算する。
 * 戻り値: record.id → (アーティスト名 → n回目)
 */
internal fun computeVisitCounts(records: List<LiveRecord>): Map<Long, Map<String, Int>> {
    val artistRunningCounts = mutableMapOf<String, Int>()
    val visitCountsById = mutableMapOf<Long, Map<String, Int>>()
    records.sortedBy { it.date }.forEach { record ->
        visitCountsById[record.id] = record.artistNames.associateWith { artistName ->
            val count = (artistRunningCounts[artistName] ?: 0) + 1
            artistRunningCounts[artistName] = count
            count
        }
    }
    return visitCountsById
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getLiveRecords: GetLiveRecordsUseCase,
    private val addLiveRecord: AddLiveRecordUseCase,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val uiState = getLiveRecords()
        .map { records ->
            val visitCountsById = computeVisitCounts(records)
            // 表示順（日付降順）は DAO の ORDER BY date DESC を維持
            val items = records.map { record ->
                LiveRecordItem(
                    id = record.id,
                    title = record.title,
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

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val records = getLiveRecords().first()
                val json = LiveRecordsJson.encode(records)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")
                        ?.use { it.write(json.toByteArray()) }
                        ?: error("failed to open output stream")
                }
                records.size
            }.onSuccess { count ->
                _message.value = "${count}件をエクスポートしました"
            }.onFailure {
                _message.value = "エクスポートに失敗しました"
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() }
                        ?: error("failed to open input stream")
                }
                val records = LiveRecordsJson.decode(text)
                records.forEach { addLiveRecord(it) }
                records.size
            }.onSuccess { count ->
                _message.value = "${count}件をインポートしました"
            }.onFailure {
                _message.value = "インポートに失敗しました（ファイル形式を確認してください）"
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
