package com.winschneid.myliverecord.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.winschneid.myliverecord.ui.screens.add.AddLiveEditPreview
import com.winschneid.myliverecord.ui.screens.add.AddLiveEmptyPreview
import com.winschneid.myliverecord.ui.screens.add.AddLiveFestivalPreview
import com.winschneid.myliverecord.ui.screens.add.AddLiveFilledPreview
import com.winschneid.myliverecord.ui.screens.artist.ArtistDetailPreview
import com.winschneid.myliverecord.ui.screens.history.HistoryEmptyPreview
import com.winschneid.myliverecord.ui.screens.history.HistoryLoadingPreview
import com.winschneid.myliverecord.ui.screens.history.HistoryNoSearchResultPreview
import com.winschneid.myliverecord.ui.screens.history.HistoryWithDataPreview
import com.winschneid.myliverecord.ui.screens.summary.YearSummaryCollapsedPreview
import com.winschneid.myliverecord.ui.screens.summary.YearSummaryExpandedPreview

/**
 * スクリーンショットテストの対象。各画面ファイルにある @Preview をそのまま呼び出して
 * 参照画像と比較する。プレビューの中身（サンプルデータ・テーマ）は各画面ファイル側で定義済み。
 *
 * 参照画像の生成: ./gradlew updateDebugScreenshotTest
 * 検証（CIで実行）: ./gradlew validateDebugScreenshotTest
 */

@Preview(showBackground = true)
@Composable
fun HistoryWithData_ss() = HistoryWithDataPreview()

@Preview(showBackground = true)
@Composable
fun HistoryEmpty_ss() = HistoryEmptyPreview()

@Preview(showBackground = true)
@Composable
fun HistoryLoading_ss() = HistoryLoadingPreview()

@Preview(showBackground = true)
@Composable
fun HistoryNoSearchResult_ss() = HistoryNoSearchResultPreview()

@Preview(showBackground = true)
@Composable
fun AddLiveEmpty_ss() = AddLiveEmptyPreview()

@Preview(showBackground = true)
@Composable
fun AddLiveFestival_ss() = AddLiveFestivalPreview()

@Preview(showBackground = true)
@Composable
fun AddLiveFilled_ss() = AddLiveFilledPreview()

@Preview(showBackground = true)
@Composable
fun AddLiveEdit_ss() = AddLiveEditPreview()

@Preview(showBackground = true)
@Composable
fun YearSummaryCollapsed_ss() = YearSummaryCollapsedPreview()

@Preview(showBackground = true)
@Composable
fun YearSummaryExpanded_ss() = YearSummaryExpandedPreview()

@Preview(showBackground = true)
@Composable
fun ArtistDetail_ss() = ArtistDetailPreview()
