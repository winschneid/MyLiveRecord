package com.example.myliverecord.ui.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myliverecord.ui.theme.MyLiveRecordTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddLiveScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddLiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    AddLiveContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLiveContent(
    uiState: AddLiveUiState,
    onAction: (AddLiveAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.date)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("削除の確認") },
            text = { Text("このライブ記録を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onAction(AddLiveAction.Delete)
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("キャンセル") }
            },
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onAction(AddLiveAction.UpdateDate(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "ライブを編集" else "ライブを記録") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // アーティスト複数入力エリア
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "アーティスト *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.artistNames.forEachIndexed { index, artistName ->
                    val suggestions = uiState.allArtistNames.filter {
                        artistName.isNotBlank() &&
                            it.contains(artistName, ignoreCase = true) &&
                            !it.equals(artistName, ignoreCase = true)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SuggestTextField(
                            value = artistName,
                            onValueChange = { onAction(AddLiveAction.UpdateArtistName(index, it)) },
                            label = if (uiState.artistNames.size == 1) "アーティスト名" else "アーティスト ${index + 1}",
                            suggestions = suggestions,
                            modifier = Modifier.weight(1f),
                        )
                        if (uiState.artistNames.size > 1) {
                            IconButton(onClick = { onAction(AddLiveAction.RemoveArtist(index)) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "削除",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { onAction(AddLiveAction.AddArtist) },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("アーティストを追加")
                }
            }

            SuggestTextField(
                value = uiState.venueName,
                onValueChange = { onAction(AddLiveAction.UpdateVenueName(it)) },
                label = "会場名 *",
                suggestions = uiState.venueSuggestions,
            )

            OutlinedTextField(
                value = uiState.seatNumber,
                onValueChange = { onAction(AddLiveAction.UpdateSeatNumber(it)) },
                label = { Text("席番号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = formatDate(uiState.date),
                onValueChange = {},
                label = { Text("日付") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("変更")
                    }
                },
            )

            Button(
                onClick = { onAction(AddLiveAction.Save) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.artistNames.any { it.isNotBlank() } && uiState.venueName.isNotBlank(),
            ) {
                Text("保存")
            }

            if (uiState.isEditMode) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("削除")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
            singleLine = true,
            trailingIcon = {
                if (suggestions.isNotEmpty()) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(timestamp))

// region Previews

@Preview(name = "登録 - 空", showBackground = true)
@Composable
private fun AddLiveEmptyPreview() {
    MyLiveRecordTheme {
        AddLiveContent(
            uiState = AddLiveUiState(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview(name = "登録 - フェス（複数アーティスト）", showBackground = true)
@Composable
private fun AddLiveFestivalPreview() {
    MyLiveRecordTheme {
        AddLiveContent(
            uiState = AddLiveUiState(
                artistNames = listOf("YOASOBI", "King Gnu", ""),
                venueName = "国立競技場",
                seatNumber = "S席 12-34",
                date = 1704067200000L,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview(name = "登録 - 入力済み", showBackground = true)
@Composable
private fun AddLiveFilledPreview() {
    MyLiveRecordTheme {
        AddLiveContent(
            uiState = AddLiveUiState(
                artistNames = listOf("YOASOBI"),
                venueName = "さいたまスーパーアリーナ",
                seatNumber = "アリーナA-12",
                date = 1704067200000L,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview(name = "編集モード", showBackground = true)
@Composable
private fun AddLiveEditPreview() {
    MyLiveRecordTheme {
        AddLiveContent(
            uiState = AddLiveUiState(
                artistNames = listOf("YOASOBI"),
                venueName = "さいたまスーパーアリーナ",
                seatNumber = "アリーナA-12",
                date = 1704067200000L,
                isEditMode = true,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

// endregion
