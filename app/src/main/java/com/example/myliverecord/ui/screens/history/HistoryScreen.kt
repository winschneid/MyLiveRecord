package com.example.myliverecord.ui.screens.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
    onNavigateToArtist: (artistName: String) -> Unit,
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
        message?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.text,
                actionLabel = if (msg.withUndo) "元に戻す" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
            viewModel.messageShown()
        }
    }

    HistoryContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateToAdd = onNavigateToAdd,
        onCardClick = onNavigateToEdit,
        onArtistClick = onNavigateToArtist,
        onDeleteRecord = viewModel::deleteRecord,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onExportClick = {
            val today = SimpleDateFormat("yyyyMMdd", Locale.JAPAN).format(Date())
            exportLauncher.launch("live_records_$today.json")
        },
        onImportClick = { importLauncher.launch(arrayOf("*/*")) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateToAdd: () -> Unit,
    onCardClick: (id: Long) -> Unit,
    onArtistClick: (artistName: String) -> Unit,
    onDeleteRecord: (id: Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                SearchTopBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = {
                        isSearchActive = false
                        onSearchQueryChange("")
                    },
                )
            } else {
                TopAppBar(
                    title = { Text("ライブ履歴") },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "検索")
                        }
                        HistoryMenu(
                            onExportClick = onExportClick,
                            onImportClick = onImportClick,
                        )
                    },
                )
            }
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
            !uiState.hasAnyRecords -> {
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
            uiState.sections.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "「${uiState.searchQuery}」に一致するライブがありません",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.sections.forEach { section ->
                        stickyHeader(key = section.label) {
                            Text(
                                text = section.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(section.items, key = { it.id }) { record ->
                            DismissibleRecordCard(
                                record = record,
                                onClick = { onCardClick(record.id) },
                                onArtistClick = onArtistClick,
                                onDelete = { onDeleteRecord(record.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("アーティスト・会場・公演名") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "クリア")
                        }
                    }
                },
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "検索を閉じる")
            }
        },
    )
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
private fun DismissibleRecordCard(
    record: LiveRecordItem,
    onClick: () -> Unit,
    onArtistClick: (artistName: String) -> Unit,
    onDelete: () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
    ) {
        LiveRecordCard(
            record = record,
            onClick = onClick,
            onArtistClick = onArtistClick,
        )
    }
}

@Composable
private fun LiveRecordCard(
    record: LiveRecordItem,
    onClick: () -> Unit,
    onArtistClick: (artistName: String) -> Unit,
) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artistName) },
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

private val previewSections = listOf(
    HistorySection(
        label = "2024年3月",
        items = listOf(
            LiveRecordItem(
                id = 3,
                title = "ROCK IN JAPAN 2024",
                artistNames = listOf("YOASOBI", "King Gnu", "Vaundy"),
                venueName = "国立競技場",
                seatNumber = "S席 12-34",
                date = 1709424000000L,
                artistVisitCounts = mapOf("YOASOBI" to 3, "King Gnu" to 1, "Vaundy" to 2),
            ),
        ),
    ),
    HistorySection(
        label = "2024年2月",
        items = listOf(
            LiveRecordItem(
                id = 2,
                artistNames = listOf("Official髭男dism"),
                venueName = "東京ドーム",
                seatNumber = "1塁側 3F-45",
                date = 1706745600000L,
                artistVisitCounts = mapOf("Official髭男dism" to 1),
            ),
        ),
    ),
    HistorySection(
        label = "2024年1月",
        items = listOf(
            LiveRecordItem(
                id = 1,
                title = "THE FILM 2",
                artistNames = listOf("YOASOBI"),
                venueName = "さいたまスーパーアリーナ",
                seatNumber = "アリーナA-12",
                date = 1704067200000L,
                artistVisitCounts = mapOf("YOASOBI" to 3),
            ),
        ),
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
            onArtistClick = {},
            onDeleteRecord = {},
            onSearchQueryChange = {},
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
            uiState = HistoryUiState(isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onNavigateToAdd = {},
            onCardClick = {},
            onArtistClick = {},
            onDeleteRecord = {},
            onSearchQueryChange = {},
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
            uiState = HistoryUiState(
                sections = previewSections,
                hasAnyRecords = true,
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateToAdd = {},
            onCardClick = {},
            onArtistClick = {},
            onDeleteRecord = {},
            onSearchQueryChange = {},
            onExportClick = {},
            onImportClick = {},
        )
    }
}

@Preview(name = "履歴 - 検索ヒットなし", showBackground = true)
@Composable
private fun HistoryNoSearchResultPreview() {
    MyLiveRecordTheme {
        HistoryContent(
            uiState = HistoryUiState(
                sections = emptyList(),
                searchQuery = "Ado",
                hasAnyRecords = true,
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateToAdd = {},
            onCardClick = {},
            onArtistClick = {},
            onDeleteRecord = {},
            onSearchQueryChange = {},
            onExportClick = {},
            onImportClick = {},
        )
    }
}

// endregion
