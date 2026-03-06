package com.example.myliverecord.ui.screens.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myliverecord.domain.model.ArtistCount
import com.example.myliverecord.domain.model.YearSummary
import com.example.myliverecord.ui.theme.MyLiveRecordTheme

@Composable
fun YearSummaryScreen(
    viewModel: YearSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    YearSummaryContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearSummaryContent(
    uiState: YearSummaryUiState,
    onAction: (YearSummaryAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("年別集計") })
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
            uiState.years.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "ライブ履歴がありません",
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
                    items(uiState.years, key = { it.year }) { summary ->
                        YearCard(
                            summary = summary,
                            isExpanded = summary.year in uiState.expandedYears,
                            onToggle = { onAction(YearSummaryAction.ToggleYear(summary.year)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearCard(
    summary: YearSummary,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_rotation",
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${summary.year}年",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${summary.totalCount}回",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "折りたたむ" else "展開する",
                        modifier = Modifier.rotate(arrowRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Expandable artist list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    HorizontalDivider()
                    summary.artists.forEachIndexed { index, artistCount ->
                        ArtistRow(rank = index + 1, artistCount = artistCount)
                        if (index < summary.artists.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistRow(rank: Int, artistCount: ArtistCount) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(20.dp),
            )
            Text(
                text = artistCount.artistName,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            text = "${artistCount.count}回",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
    }
}

// region Previews

private val previewYears = listOf(
    YearSummary(
        year = 2025,
        totalCount = 3,
        artists = listOf(
            ArtistCount("Ado", 2),
            ArtistCount("King Gnu", 1),
        ),
    ),
    YearSummary(
        year = 2024,
        totalCount = 12,
        artists = listOf(
            ArtistCount("YOASOBI", 5),
            ArtistCount("Official髭男dism", 4),
            ArtistCount("Ado", 3),
        ),
    ),
)

@Preview(name = "年別集計 - 折りたたみ", showBackground = true)
@Composable
private fun YearSummaryCollapsedPreview() {
    MyLiveRecordTheme {
        YearSummaryContent(
            uiState = YearSummaryUiState(years = previewYears, isLoading = false),
            onAction = {},
        )
    }
}

@Preview(name = "年別集計 - 展開", showBackground = true)
@Composable
private fun YearSummaryExpandedPreview() {
    MyLiveRecordTheme {
        YearSummaryContent(
            uiState = YearSummaryUiState(
                years = previewYears,
                isLoading = false,
                expandedYears = setOf(2024, 2025),
            ),
            onAction = {},
        )
    }
}

// endregion
