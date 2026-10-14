package com.winschneid.myliverecord.ui.screens.backup

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winschneid.myliverecord.data.auth.DriveAuthorizer
import com.winschneid.myliverecord.data.auth.GoogleAccount
import com.winschneid.myliverecord.data.auth.GoogleAccountRepository
import com.winschneid.myliverecord.data.auth.GoogleSignInClient
import com.winschneid.myliverecord.data.auth.SignInError
import com.winschneid.myliverecord.data.backup.CloudBackupRepository
import com.winschneid.myliverecord.data.backup.ConsentRequiredException
import com.winschneid.myliverecord.data.backup.DriveError
import com.winschneid.myliverecord.data.backup.RemoteBackup
import com.winschneid.myliverecord.data.backup.RestoreCandidate
import com.winschneid.myliverecord.domain.usecase.GetLiveRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException
import javax.inject.Inject

enum class BackupBusy { None, SigningIn, Checking, BackingUp, Downloading, Restoring }

sealed interface BackupDialog {
    data class ConfirmBackup(val localCount: Int, val existingModifiedAt: Long?) : BackupDialog
    data class ConfirmRestore(val remoteCount: Int, val modifiedAt: Long, val localCount: Int) : BackupDialog
    data object ConfirmSignOut : BackupDialog
}

data class BackupUiState(
    /** ビルドに Web クライアントIDが設定されているか */
    val isConfigured: Boolean = true,
    val account: GoogleAccount? = null,
    val localCount: Int = 0,
    /** この端末から最後にバックアップした日時（epoch millis）。Drive 未確認時の目安に使う */
    val lastBackupAt: Long? = null,
    /** Drive 上のバックアップ。未確認（まだ問い合わせていない／失敗）なら null */
    val remote: RemoteBackup? = null,
    val remoteChecked: Boolean = false,
    val busy: BackupBusy = BackupBusy.None,
    val dialog: BackupDialog? = null,
)

/** 一回限りのイベント。PendingIntent の起動は必ず1回なので StateFlow ではなく Channel で流す。 */
sealed interface BackupEvent {
    data class LaunchConsent(val pendingIntent: PendingIntent) : BackupEvent
    data class Message(val text: String) : BackupEvent
}

private enum class PendingAction(val busy: BackupBusy) {
    Refresh(BackupBusy.Checking),
    Backup(BackupBusy.BackingUp),
    Restore(BackupBusy.Downloading),
}

private data class TransientState(
    val remote: RemoteBackup? = null,
    val remoteChecked: Boolean = false,
    val busy: BackupBusy = BackupBusy.None,
    val dialog: BackupDialog? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val signInClient: GoogleSignInClient,
    private val accountRepository: GoogleAccountRepository,
    private val backupRepository: CloudBackupRepository,
    private val authorizer: DriveAuthorizer,
    getLiveRecords: GetLiveRecordsUseCase,
) : ViewModel() {

    private val transient = MutableStateFlow(TransientState())
    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val account = accountRepository.account
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 同意画面から戻った後にやり直す操作 */
    private var pendingAction: PendingAction? = null

    /** 確認ダイアログ表示中の復元候補 */
    private var restoreCandidate: RestoreCandidate? = null

    val uiState = combine(
        account,
        getLiveRecords().map { it.size },
        accountRepository.lastBackupAt,
        transient,
    ) { account, localCount, lastBackupAt, transient ->
        BackupUiState(
            isConfigured = signInClient.isConfigured,
            account = account,
            localCount = localCount,
            lastBackupAt = lastBackupAt,
            remote = transient.remote,
            remoteChecked = transient.remoteChecked,
            busy = transient.busy,
            dialog = transient.dialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupUiState(isConfigured = signInClient.isConfigured),
    )

    init {
        // サインイン済みなら Drive 上のバックアップ状況を取得する（サインイン直後も同様）
        viewModelScope.launch {
            account.map { it?.email }.distinctUntilChanged().filterNotNull().collect {
                run(PendingAction.Refresh)
            }
        }
    }

    /** アカウント選択 UI を出すので Activity の Context を渡す。保持はしない。 */
    fun signIn(activityContext: Context) {
        if (!beginBusy(BackupBusy.SigningIn)) return
        viewModelScope.launch {
            try {
                val signedIn = signInClient.signIn(activityContext)
                accountRepository.save(signedIn)
                message("${signedIn.email} でサインインしました")
            } catch (e: SignInError.Cancelled) {
                // ユーザー操作による中断なので何も出さない
            } catch (e: SignInError.NoAccount) {
                message("端末にGoogleアカウントがありません")
            } catch (e: SignInError.NotConfigured) {
                message("Google連携が設定されていません")
            } catch (e: Exception) {
                message("サインインに失敗しました")
            } finally {
                endBusy()
            }
        }
    }

    fun requestSignOut() {
        transient.update { it.copy(dialog = BackupDialog.ConfirmSignOut) }
    }

    fun confirmSignOut() {
        dismissDialog()
        viewModelScope.launch {
            runCatching { signInClient.signOut() }
            accountRepository.clear()
            pendingAction = null
            restoreCandidate = null
            transient.update { it.copy(remote = null, remoteChecked = false) }
            message("サインアウトしました")
        }
    }

    fun requestBackup() {
        val state = uiState.value
        if (state.account == null || state.busy != BackupBusy.None) return
        transient.update {
            it.copy(dialog = BackupDialog.ConfirmBackup(state.localCount, state.remote?.modifiedAt))
        }
    }

    fun confirmBackup() {
        dismissDialog()
        run(PendingAction.Backup)
    }

    fun requestRestore() {
        run(PendingAction.Restore)
    }

    fun confirmRestore() {
        val candidate = restoreCandidate ?: return
        dismissDialog()
        if (!beginBusy(BackupBusy.Restoring)) return
        viewModelScope.launch {
            try {
                val count = backupRepository.applyRestore(candidate)
                restoreCandidate = null
                message("${count}件を復元しました")
            } catch (e: Exception) {
                message("復元に失敗しました")
            } finally {
                endBusy()
            }
        }
    }

    fun dismissDialog() {
        restoreCandidate = null
        transient.update { it.copy(dialog = null) }
    }

    /** Drive の同意画面から戻ったとき。許可されていれば中断していた操作をやり直す。 */
    fun onConsentResult(resultCode: Int, data: Intent?) {
        val action = pendingAction
        pendingAction = null
        val granted = resultCode == Activity.RESULT_OK &&
            runCatching { authorizer.resultFromIntent(data) }.isSuccess
        if (granted && action != null) {
            run(action)
        } else {
            message("アクセスが許可されませんでした")
        }
    }

    private fun run(action: PendingAction) {
        val email = account.value?.email ?: return
        if (!beginBusy(action.busy)) return
        viewModelScope.launch {
            try {
                when (action) {
                    PendingAction.Refresh -> {
                        val remote = backupRepository.fetchRemoteInfo(email)
                        transient.update { it.copy(remote = remote, remoteChecked = true) }
                    }
                    PendingAction.Backup -> {
                        val result = backupRepository.backup(email)
                        transient.update { it.copy(remote = result.remote, remoteChecked = true) }
                        message("${result.count}件をバックアップしました")
                    }
                    PendingAction.Restore -> {
                        val candidate = backupRepository.fetchRestoreCandidate(email)
                        if (candidate == null) {
                            transient.update { it.copy(remote = null, remoteChecked = true) }
                            message("バックアップが見つかりません")
                        } else {
                            restoreCandidate = candidate
                            transient.update {
                                it.copy(
                                    remote = candidate.remote,
                                    remoteChecked = true,
                                    dialog = BackupDialog.ConfirmRestore(
                                        remoteCount = candidate.records.size,
                                        modifiedAt = candidate.remote.modifiedAt,
                                        localCount = uiState.value.localCount,
                                    ),
                                )
                            }
                        }
                    }
                }
            } catch (e: ConsentRequiredException) {
                pendingAction = action
                _events.send(BackupEvent.LaunchConsent(e.pendingIntent))
            } catch (e: Exception) {
                message(errorMessage(action, e))
            } finally {
                endBusy()
            }
        }
    }

    private fun errorMessage(action: PendingAction, e: Exception): String {
        val verb = when (action) {
            PendingAction.Refresh -> "バックアップの確認"
            PendingAction.Backup -> "バックアップ"
            PendingAction.Restore -> "復元"
        }
        return when (e) {
            is DriveError.Network -> "ネットワークに接続できません"
            is DriveError.Unauthorized -> "再度サインインしてください"
            is DriveError.Forbidden -> "Googleドライブにアクセスできません（設定を確認してください）"
            is DriveError.Http -> "${verb}に失敗しました（HTTP ${e.code}）"
            is JSONException, is IllegalArgumentException -> "バックアップの形式が不正です"
            else -> "${verb}に失敗しました"
        }
    }

    /** 他の処理が走っていなければ busy にして true。走っていれば false。 */
    private fun beginBusy(busy: BackupBusy): Boolean {
        var started = false
        transient.update {
            if (it.busy == BackupBusy.None) {
                started = true
                it.copy(busy = busy)
            } else {
                it
            }
        }
        return started
    }

    private fun endBusy() {
        transient.update { it.copy(busy = BackupBusy.None) }
    }

    private fun message(text: String) {
        _events.trySend(BackupEvent.Message(text))
    }
}
