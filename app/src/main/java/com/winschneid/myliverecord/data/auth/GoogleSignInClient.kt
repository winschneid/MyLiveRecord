package com.winschneid.myliverecord.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.winschneid.myliverecord.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SignInError(message: String) : Exception(message) {
    /** ユーザーがアカウント選択を閉じた */
    data object Cancelled : SignInError("sign-in cancelled")

    /** 端末に Google アカウントが無い */
    data object NoAccount : SignInError("no google account on device")

    /** ビルドに Web クライアントIDが設定されていない */
    data object NotConfigured : SignInError("GOOGLE_WEB_CLIENT_ID is empty")
}

/** Credential Manager による Google サインイン（「誰か」の確認）。Drive へのアクセス許可は DriveAuthorizer。 */
@Singleton
class GoogleSignInClient @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()

    /**
     * アカウント選択 UI を出すため Activity の Context が必要（保持はしない）。
     * 失敗時は [SignInError]、それ以外の予期しないエラーは GetCredentialException をそのまま投げる。
     */
    suspend fun signIn(activityContext: Context): GoogleAccount {
        if (!isConfigured) throw SignInError.NotConfigured
        val credentialManager = CredentialManager.create(activityContext)

        suspend fun request(onlyAuthorizedAccounts: Boolean): GetCredentialResponse {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(onlyAuthorizedAccounts)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(onlyAuthorizedAccounts)
                .build()
            return credentialManager.getCredential(
                context = activityContext,
                request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
            )
        }

        val response = try {
            // まずは以前このアプリを許可したアカウントだけに絞る（サインアウト後の再サインインが速い）
            try {
                request(onlyAuthorizedAccounts = true)
            } catch (e: NoCredentialException) {
                request(onlyAuthorizedAccounts = false)
            }
        } catch (e: GetCredentialCancellationException) {
            throw SignInError.Cancelled
        } catch (e: NoCredentialException) {
            throw SignInError.NoAccount
        }

        val credential = response.credential
        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "unexpected credential type: ${credential.type}" }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleAccount(
            email = googleCredential.id,
            displayName = googleCredential.displayName,
        )
    }

    /** 次回サインイン時にアカウント選択を再表示させる。 */
    suspend fun signOut() {
        CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
    }
}
