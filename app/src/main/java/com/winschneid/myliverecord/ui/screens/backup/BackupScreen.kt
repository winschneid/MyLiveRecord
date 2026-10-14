package com.winschneid.myliverecord.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winschneid.myliverecord.data.auth.GoogleAccount
import com.winschneid.myliverecord.data.backup.RemoteBackup
import com.winschneid.myliverecord.ui.theme.MyLiveRecordTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // setContent 配下なので Activity。サインイン UI の起動にだけ使う
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> viewModel.onConsentResult(result.resultCode, result.data) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BackupEvent.LaunchConsent -> consentLauncher.launch(
                    IntentSenderRequest.Builder(event.pendingIntent.intentSender).build()
                )
                is BackupEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    BackupContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onSignIn = { viewModel.signIn(context) },
        onSignOut = viewModel::requestSignOut,
        onBackup = viewModel::requestBackup,
        onRestore = viewModel::requestRestore,
        onConfirmBackup = viewModel::confirmBackup,
        onConfirmRestore = viewModel::confirmRestore,
        onConfirmSignOut = viewModel::confirmSignOut,
        onDismissDialog = viewModel::dismissDialog,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupContent(
    uiState: BackupUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onConfirmBackup: () -> Unit,
    onConfirmRestore: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    uiState.dialog?.let { dialog ->
        BackupDialogs(
            dialog = dialog,
            onConfirmBackup = onConfirmBackup,
            onConfirmRestore = onConfirmRestore,
            onConfirmSignOut = onConfirmSignOut,
            onDismiss = onDismissDialog,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("バックアップ") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                !uiState.isConfigured -> NotConfiguredCard()
                uiState.account == null -> SignedOutSection(
                    busy = uiState.busy,
                    onSignIn = onSignIn,
                )
                else -> SignedInSection(
                    uiState = uiState,
                    account = uiState.account,
                    onSignOut = onSignOut,
                    onBackup = onBackup,
                    onRestore = onRestore,
                )
            }
        }
    }
}

@Composable
private fun NotConfiguredCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Google連携が設定されていません", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "この端末のビルドには Google サインイン用の設定が含まれていないため、" +
                    "クラウドバックアップは利用できません。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignedOutSection(
    busy: BackupBusy,
    onSignIn: () -> Unit,
) {
    Text(
        "Googleアカウントでサインインすると、記録を Google ドライブのアプリ専用領域に" +
            "バックアップし、別の端末で復元できます。バックアップはあなたのドライブにだけ保存され、" +
            "開発者がアクセスすることはありません。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = onSignIn,
        enabled = busy == BackupBusy.None,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (busy == BackupBusy.SigningIn) "サインイン中…" else "Googleでサインイン")
    }
}

@Composable
private fun SignedInSection(
    uiState: BackupUiState,
    account: GoogleAccount,
    onSignOut: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val idle = uiState.busy == BackupBusy.None

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                account.displayName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    account.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSignOut, enabled = idle) { Text("サインアウト") }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Googleドライブ上のバックアップ", style = MaterialTheme.typography.titleSmall)
        Text(
            text = when {
                uiState.remoteChecked && uiState.remote == null -> "バックアップはまだありません"
                uiState.remoteChecked && uiState.remote != null ->
                    "最終バックアップ: ${formatDateTime(uiState.remote.modifiedAt)}"
                uiState.busy == BackupBusy.Checking -> "確認中…"
                uiState.lastBackupAt != null ->
                    "未確認（この端末からの最終バックアップ: ${formatDateTime(uiState.lastBackupAt)}）"
                else -> "未確認"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Button(
        onClick = onBackup,
        enabled = idle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (uiState.busy == BackupBusy.BackingUp) "バックアップ中…"
            else "今すぐバックアップ（${uiState.localCount}件）"
        )
    }
    OutlinedButton(
        onClick = onRestore,
        enabled = idle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            when (uiState.busy) {
                BackupBusy.Downloading -> "ダウンロード中…"
                BackupBusy.Restoring -> "復元中…"
                else -> "バックアップから復元"
            }
        )
    }
    if (!idle) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Text(
        "復元は端末内の記録を消さずに追加します。同じ記録が二重になる場合があるので、" +
            "新しい端末での初回復元に使うことをおすすめします。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BackupDialogs(
    dialog: BackupDialog,
    onConfirmBackup: () -> Unit,
    onConfirmRestore: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is BackupDialog.ConfirmBackup -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("バックアップの確認") },
            text = {
                Text(
                    buildString {
                        append("現在の記録 ${dialog.localCount} 件を Google ドライブにバックアップします。")
                        dialog.existingModifiedAt?.let {
                            append("\n\n前回のバックアップ（${formatDateTime(it)}）は上書きされます。")
                        }
                    }
                )
            },
            confirmButton = { TextButton(onClick = onConfirmBackup) { Text("バックアップ") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        )
        is BackupDialog.ConfirmRestore -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("復元の確認") },
            text = {
                Text(
                    "バックアップ（${formatDateTime(dialog.modifiedAt)}）の ${dialog.remoteCount} 件を、" +
                        "現在の ${dialog.localCount} 件に追加します。\n\n" +
                        "端末内の記録は削除されませんが、同じ記録が重複する可能性があります。"
                )
            },
            confirmButton = { TextButton(onClick = onConfirmRestore) { Text("復元") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        )
        BackupDialog.ConfirmSignOut -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("サインアウトの確認") },
            text = { Text("サインアウトします。端末内の記録とドライブ上のバックアップは削除されません。") },
            confirmButton = {
                TextButton(onClick = onConfirmSignOut) {
                    Text("サインアウト", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        )
    }
}

private fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(epochMillis))

// ---- Previews ----

private val previewAccount = GoogleAccount(email = "example@gmail.com", displayName = "山田 太郎")
private const val PREVIEW_MODIFIED_AT = 1_780_000_000_000L // 2026-06 頃

@Preview(name = "バックアップ: 未設定", showBackground = true)
@Composable
internal fun BackupNotConfiguredPreview() {
    MyLiveRecordTheme {
        BackupContent(
            uiState = BackupUiState(isConfigured = false),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {}, onSignIn = {}, onSignOut = {}, onBackup = {}, onRestore = {},
            onConfirmBackup = {}, onConfirmRestore = {}, onConfirmSignOut = {}, onDismissDialog = {},
        )
    }
}

@Preview(name = "バックアップ: 未サインイン", showBackground = true)
@Composable
internal fun BackupSignedOutPreview() {
    MyLiveRecordTheme {
        BackupContent(
            uiState = BackupUiState(isConfigured = true, account = null, localCount = 12),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {}, onSignIn = {}, onSignOut = {}, onBackup = {}, onRestore = {},
            onConfirmBackup = {}, onConfirmRestore = {}, onConfirmSignOut = {}, onDismissDialog = {},
        )
    }
}

@Preview(name = "バックアップ: サインイン済み", showBackground = true)
@Composable
internal fun BackupSignedInPreview() {
    MyLiveRecordTheme {
        BackupContent(
            uiState = BackupUiState(
                isConfigured = true,
                account = previewAccount,
                localCount = 12,
                remote = RemoteBackup(fileId = "abc", modifiedAt = PREVIEW_MODIFIED_AT, sizeBytes = 2048),
                remoteChecked = true,
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {}, onSignIn = {}, onSignOut = {}, onBackup = {}, onRestore = {},
            onConfirmBackup = {}, onConfirmRestore = {}, onConfirmSignOut = {}, onDismissDialog = {},
        )
    }
}

@Preview(name = "バックアップ: 復元確認", showBackground = true)
@Composable
internal fun BackupConfirmRestorePreview() {
    MyLiveRecordTheme {
        BackupContent(
            uiState = BackupUiState(
                isConfigured = true,
                account = previewAccount,
                localCount = 3,
                remote = RemoteBackup(fileId = "abc", modifiedAt = PREVIEW_MODIFIED_AT, sizeBytes = 2048),
                remoteChecked = true,
                dialog = BackupDialog.ConfirmRestore(remoteCount = 12, modifiedAt = PREVIEW_MODIFIED_AT, localCount = 3),
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack = {}, onSignIn = {}, onSignOut = {}, onBackup = {}, onRestore = {},
            onConfirmBackup = {}, onConfirmRestore = {}, onConfirmSignOut = {}, onDismissDialog = {},
        )
    }
}
