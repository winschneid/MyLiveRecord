package com.winschneid.myliverecord.domain.usecase

import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.transfer.LiveRecordsJson
import javax.inject.Inject

/**
 * LiveRecordsJson 形式の文字列を記録として取り込む（既存記録には追記。重複除去はしない）。
 * 解析と書き込みを分けているのは、クラウド復元で「件数を確認ダイアログに出してから書き込む」ため。
 */
class ImportLiveRecordsJsonUseCase @Inject constructor(
    private val addLiveRecord: AddLiveRecordUseCase,
) {
    /** 不正な形式なら IllegalArgumentException / JSONException を投げる。書き込みは行わない。 */
    fun parse(text: String): List<LiveRecord> = LiveRecordsJson.decode(text)

    /** 解析済みの記録を追記し、追加件数を返す。 */
    suspend fun apply(records: List<LiveRecord>): Int {
        records.forEach { addLiveRecord(it) }
        return records.size
    }

    suspend operator fun invoke(text: String): Int = apply(parse(text))
}
