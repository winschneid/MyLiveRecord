package com.winschneid.myliverecord.data.auth

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface AuthOutcome {
    /** すぐ使えるアクセストークン */
    data class Token(val accessToken: String) : AuthOutcome

    /** ユーザーの同意画面を出す必要がある。Activity から PendingIntent を起動する */
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthOutcome
}

/** Google Drive（appDataFolder）へのアクセス許可とアクセストークンの取得。 */
interface DriveAuthorizer {
    suspend fun authorize(email: String): AuthOutcome

    /** 同意画面から戻った Intent を検証する。拒否・失敗なら例外（ApiException）。 */
    fun resultFromIntent(data: Intent?): String

    /** 失効したトークンをキャッシュから捨てる（次の authorize で取り直す）。 */
    suspend fun invalidate(accessToken: String)
}

@Singleton
class PlayServicesDriveAuthorizer @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : DriveAuthorizer {

    private val client get() = Identity.getAuthorizationClient(appContext)

    override suspend fun authorize(email: String): AuthOutcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
            .build()
        val result = client.authorize(request).await()
        val pendingIntent = result.pendingIntent
        return if (result.hasResolution() && pendingIntent != null) {
            AuthOutcome.NeedsConsent(pendingIntent)
        } else {
            AuthOutcome.Token(checkNotNull(result.accessToken) { "no access token" })
        }
    }

    override fun resultFromIntent(data: Intent?): String {
        val result = client.getAuthorizationResultFromIntent(data)
        return checkNotNull(result.accessToken) { "no access token" }
    }

    override suspend fun invalidate(accessToken: String) {
        client.clearToken(ClearTokenRequest.builder().setToken(accessToken).build()).await()
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}

/** kotlinx-coroutines-play-services を追加せずに Task を suspend で待つ。 */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val e = task.exception
        when {
            e != null -> cont.resumeWithException(e)
            task.isCanceled -> cont.cancel()
            else -> cont.resume(task.result)
        }
    }
}
