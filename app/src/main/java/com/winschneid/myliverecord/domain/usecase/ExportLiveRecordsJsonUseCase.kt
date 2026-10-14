package com.winschneid.myliverecord.domain.usecase

import com.winschneid.myliverecord.domain.transfer.LiveRecordsJson
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class ExportResult(
    val json: String,
    val count: Int,
)

/** 全記録を LiveRecordsJson 形式の文字列にする。ファイル書き出しとクラウドバックアップで共用。 */
class ExportLiveRecordsJsonUseCase @Inject constructor(
    private val getLiveRecords: GetLiveRecordsUseCase,
) {
    suspend operator fun invoke(): ExportResult {
        val records = getLiveRecords().first()
        return ExportResult(json = LiveRecordsJson.encode(records), count = records.size)
    }
}
