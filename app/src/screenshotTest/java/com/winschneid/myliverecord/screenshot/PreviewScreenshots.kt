package com.winschneid.myliverecord.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
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
 * 対象にするには @PreviewTest が必要（プラグイン 0.0.1-alpha10 以降、無いものは描画されない）。
 *
 * 参照画像の生成: ./gradlew updateDebugScreenshotTest（出力先: app/src/screenshotTestDebug/reference）
 * 検証（CIで実行）: ./gradlew validateDebugScreenshotTest
 */

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryWithData_ss() = HistoryWithDataPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryEmpty_ss() = HistoryEmptyPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryLoading_ss() = HistoryLoadingPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryNoSearchResult_ss() = HistoryNoSearchResultPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddLiveEmpty_ss() = AddLiveEmptyPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddLiveFestival_ss() = AddLiveFestivalPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddLiveFilled_ss() = AddLiveFilledPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddLiveEdit_ss() = AddLiveEditPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun YearSummaryCollapsed_ss() = YearSummaryCollapsedPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun YearSummaryExpanded_ss() = YearSummaryExpandedPreview()

@PreviewTest
@Preview(showBackground = true)
@Composable
fun ArtistDetail_ss() = ArtistDetailPreview()
