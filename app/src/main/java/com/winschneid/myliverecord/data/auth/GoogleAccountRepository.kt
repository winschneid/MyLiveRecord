package com.winschneid.myliverecord.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** サインイン状態と最終バックアップ日時。設定と同じ DataStore に保存する。 */
@Singleton
class GoogleAccountRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val emailKey = stringPreferencesKey("google_email")
    private val displayNameKey = stringPreferencesKey("google_display_name")
    private val lastBackupAtKey = longPreferencesKey("last_backup_at")

    val account: Flow<GoogleAccount?> = dataStore.data.map { prefs ->
        prefs[emailKey]?.let { email -> GoogleAccount(email = email, displayName = prefs[displayNameKey]) }
    }

    /** 最後にこの端末からバックアップした日時（epoch millis）。未実施なら null。 */
    val lastBackupAt: Flow<Long?> = dataStore.data.map { it[lastBackupAtKey] }

    suspend fun save(account: GoogleAccount) {
        dataStore.edit { prefs ->
            prefs[emailKey] = account.email
            account.displayName?.let { prefs[displayNameKey] = it } ?: prefs.remove(displayNameKey)
        }
    }

    suspend fun setLastBackupAt(epochMillis: Long) {
        dataStore.edit { it[lastBackupAtKey] = epochMillis }
    }

    /** サインアウト。端末内の記録には触れない。 */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(emailKey)
            prefs.remove(displayNameKey)
            prefs.remove(lastBackupAtKey)
        }
    }
}
