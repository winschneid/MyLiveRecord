package com.winschneid.myliverecord.data.backup

import android.app.PendingIntent
import com.winschneid.myliverecord.data.auth.AuthOutcome
import com.winschneid.myliverecord.data.auth.DriveAuthorizer
import com.winschneid.myliverecord.data.auth.GoogleAccountRepository
import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.usecase.ExportLiveRecordsJsonUseCase
import com.winschneid.myliverecord.domain.usecase.ImportLiveRecordsJsonUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Drive へのアクセスにユーザーの同意が必要。呼び出し側で PendingIntent を起動し、戻ったら操作をやり直す。 */
class ConsentRequiredException(val pendingIntent: PendingIntent) : Exception("drive consent required")

data class BackupResult(
    val count: Int,
    val remote: RemoteBackup,
)

/** ダウンロード・解析済みでまだ書き込んでいない復元候補。 */
data class RestoreCandidate(
    val records: List<LiveRecord>,
    val remote: RemoteBackup,
)

@Singleton
class CloudBackupRepository @Inject constructor(
    private val authorizer: DriveAuthorizer,
    private val client: DriveAppDataClient,
    private val exportLiveRecordsJson: ExportLiveRecordsJsonUseCase,
    private val importLiveRecordsJson: ImportLiveRecordsJsonUseCase,
    private val accountRepository: GoogleAccountRepository,
) {

    suspend fun fetchRemoteInfo(email: String): RemoteBackup? =
        withToken(email) { token -> client.findBackup(token) }

    /** 全記録を書き出して Drive 上のバックアップを作成または上書きする。 */
    suspend fun backup(email: String): BackupResult {
        val export = exportLiveRecordsJson()
        val remote = withToken(email) { token ->
            val existing = client.findBackup(token)
            if (existing == null) {
                client.create(token, export.json)
            } else {
                client.update(token, existing.fileId, export.json)
            }
        }
        accountRepository.setLastBackupAt(remote.modifiedAt)
        return BackupResult(count = export.count, remote = remote)
    }

    /** バックアップを取得して解析する（書き込みはしない）。無ければ null。形式不正は例外。 */
    suspend fun fetchRestoreCandidate(email: String): RestoreCandidate? {
        val (remote, text) = withToken(email) { token ->
            val remote = client.findBackup(token) ?: return@withToken null
            remote to client.download(token, remote.fileId)
        } ?: return null
        return RestoreCandidate(records = importLiveRecordsJson.parse(text), remote = remote)
    }

    /** 復元候補を既存記録に追記する（既存ファイルインポートと同じ挙動）。 */
    suspend fun applyRestore(candidate: RestoreCandidate): Int =
        importLiveRecordsJson.apply(candidate.records)

    /**
     * トークンを取って [block] を実行する。401 なら一度だけトークンを捨てて取り直す。
     * 同意が必要なら [ConsentRequiredException]。
     */
    private suspend fun <T> withToken(email: String, block: (String) -> T): T {
        val token = obtainToken(email)
        return try {
            withContext(Dispatchers.IO) { block(token) }
        } catch (e: DriveError.Unauthorized) {
            authorizer.invalidate(token)
            val fresh = obtainToken(email)
            withContext(Dispatchers.IO) { block(fresh) }
        }
    }

    private suspend fun obtainToken(email: String): String =
        when (val outcome = authorizer.authorize(email)) {
            is AuthOutcome.Token -> outcome.accessToken
            is AuthOutcome.NeedsConsent -> throw ConsentRequiredException(outcome.pendingIntent)
        }
}
