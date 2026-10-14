package com.winschneid.myliverecord.data.auth

/** サインイン済みの Google アカウント（表示用に端末内へ保存する最小限の情報）。 */
data class GoogleAccount(
    val email: String,
    val displayName: String?,
)
