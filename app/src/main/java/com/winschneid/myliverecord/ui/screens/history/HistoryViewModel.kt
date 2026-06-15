package com.winschneid.myliverecord.ui.screens.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winschneid.myliverecord.data.transfer.LiveRecordsJson
import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.usecase.AddLiveRecordUseCase
import com.winschneid.myliverecord.domain.usecase.DeleteLiveRecordUseCase
import com.winschneid.myliverecord.domain.usecase.GetLiveRecordByIdUseCase
import com.winschneid.myliverecord.domain.usecase.GetLiveRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

data class HistorySection(
    val label: String, // 例: 2026年6月
    val items: List<LiveRecordItem>,
)

data class HistoryUiState(
    val sections: List<HistorySection> = emptyList(),
    val searchQuery: String = "",
    val hasAnyRecords: Boolean = false,
    val isLoading: Boolean = true,
)

data class HistoryMessage(
    val text: String,
    val withUndo: Boolean = false,
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

/**
 * 検索フィルタを適用し、年月ごとのセクションに分ける。
 * 「n回目」はフィルタ前の全履歴を基準に計算するため、検索しても値は変わらない。
 * records は日付降順前提（DAO の ORDER BY を維持）。
 */
internal fun buildHistorySections(records: List<LiveRecord>, query: String): List<HistorySection> {
    val visitCountsById = computeVisitCounts(records)
    val trimmed = query.trim()
    val filtered = if (trimmed.isEmpty()) records else records.filter { record ->
        record.artistNames.any { it.contains(trimmed, ignoreCase = true) } ||
            record.venueName.contains(trimmed, ignoreCase = true) ||
            record.title.contains(trimmed, ignoreCase = true)
    }
    val monthFormat = SimpleDateFormat("yyyy年M月", Locale.JAPAN)
    return filtered
        .map { record ->
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
        .groupBy { monthFormat.format(Date(it.date)) }
        .map { (label, items) -> HistorySection(label, items) }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getLiveRecords: GetLiveRecordsUseCase,
    private val addLiveRecord: AddLiveRecordUseCase,
    private val deleteLiveRecord: DeleteLiveRecordUseCase,
    private val getLiveRecordById: GetLiveRecordByIdUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<HistoryMessage?>(null)
    val message = _message.asStateFlow()

    private var pendingUndo: LiveRecord? = null

    val uiState = combine(getLiveRecords(), _searchQuery) { records, query ->
        HistoryUiState(
            sections = buildHistorySections(records, query),
            searchQuery = query,
            hasAnyRecords = records.isNotEmpty(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            val record = getLiveRecordById(id) ?: return@launch
            deleteLiveRecord(id)
            pendingUndo = record
            _message.value = HistoryMessage("削除しました", withUndo = true)
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            pendingUndo?.let { addLiveRecord(it.copy(id = 0)) }
            pendingUndo = null
        }
    }

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
                _message.value = HistoryMessage("${count}件をエクスポートしました")
            }.onFailure {
                _message.value = HistoryMessage("エクスポートに失敗しました")
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
                _message.value = HistoryMessage("${count}件をインポートしました")
            }.onFailure {
                _message.value = HistoryMessage("インポートに失敗しました（ファイル形式を確認してください）")
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
