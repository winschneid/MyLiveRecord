package com.example.myliverecord.ui.screens.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myliverecord.ui.theme.MyLiveRecordTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (id: Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importFrom) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    HistoryContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateToAdd = onNavigateToAdd,
        onCardClick = onNavigateToEdit,
        onExportClick = {
            val today = SimpleDateFormat("yyyyMMdd", Locale.JAPAN).format(Date())
            exportLauncher.launch("live_records_$today.json")
        },
        onImportClick = { importLauncher.launch(arrayOf("*/*")) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateToAdd: () -> Unit,
    onCardClick: (id: Long) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ライブ履歴") },
                actions = {
                    HistoryMenu(
                        onExportClick = onExportClick,
                        onImportClick = onImportClick,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "追加")
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.records.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "ライブ履歴がありません\n＋ボタンで追加してください",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.records, key = { it.id }) { record ->
                        LiveRecordCard(
                            record = record,
                            onClick = { onCardClick(record.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMenu(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "メニュー")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("エクスポート (JSON)") },
            onClick = {
                expanded = false
                onExportClick()
            },
        )
        DropdownMenuItem(
            text = { Text("インポート (JSON)") },
            onClick = {
                expanded = false
                onImportClick()
            },
        )
    }
}

@Composable
private fun LiveRecordCard(record: LiveRecordItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (record.title.isNotBlank()) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            record.artistNames.forEach { artistName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = "${record.artistVisitCounts[artistName] ?: 1}回目",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
            Text(
                text = record.venueName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "席: ${record.seatNumber.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDate(record.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(timestamp))

// region Previews

private val previewRecords = listOf(
    LiveRecordItem(
        id = 1,
        title = "THE FILM 2",
        artistNames = listOf("YOASOBI"),
        venueName = "さいたまスーパーアリーナ",
        seatNumber = "アリーナA-12",
        date = 1704067200000L,
        artistVisitCounts = mapOf("YOASOBI" to 3),
    ),
    LiveRecordItem(
        id = 2,
        artistNames = listOf("Official髭男dism"),
        venueName = "東京ドーム",
        seatNumber = "1塁側 3F-45",
        date = 1706745600000L,
        artistVisitCounts = mapOf("Official髭男dism" to 1),
    ),
    LiveRecordItem(
        id = 3,
        title = "ROCK IN JAPAN 2024",
        artistNames = listOf("YOASOBI", "King Gnu", "Vaundy"),
        venueName = "国立競技場",
        seatNumber = "S席 12-34",
        date = 1709424000000L,
        artistVisitCounts = mapOf("YOASOBI" to 3, "King Gnu" to 1, "Vaundy" to 2),
    ),
)

@Preview(name = "履歴 - ローディング", showBackground = true)
@Composable
private fun HistoryLoadingPreview() {
    MyLiveRecordTheme {
        HistoryContent(
            uiState = HistoryUiState(isLoading = true),
            snackbarHostState = SnackbarHostState(),
            onNavigateToAdd = {},
            onCardClick = {},
            onExportClick = {},
            onImportClick = {},
        )
    }
}

@Preview(name = "履歴 - 空", showBackground = true)
@Composable
private fun HistoryEmptyPreview() {
    MyLiveRecordTheme {
        HistoryContent(
            uiState = HistoryUiState(records = emptyList(), isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onNavigateToAdd = {},
            onCardClick = {},
            onExportClick = {},
            onImportClick = {},
        )
    }
}

@Preview(name = "履歴 - データあり", showBackground = true)
@Composable
private fun HistoryWithDataPreview() {
    MyLiveRecordTheme {
        HistoryContent(
            uiState = HistoryUiState(records = previewRecords, isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onNavigateToAdd = {},
            onCardClick = {},
            onExportClick = {},
            onImportClick = {},
        )
    }
}

@Preview(name = "履歴カード", showBackground = true)
@Composable
private fun LiveRecordCardPreview() {
    MyLiveRecordTheme {
        LiveRecordCard(record = previewRecords.first(), onClick = {})
    }
}

// endregion
