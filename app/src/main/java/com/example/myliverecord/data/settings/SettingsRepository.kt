package com.example.myliverecord.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** アプリの表示設定。DataStoreで永続化する。 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val showSpendingKey = booleanPreferencesKey("show_spending")

    /** 集計画面に支出（チケット代）を表示するか。既定はオフ。 */
    val showSpending: Flow<Boolean> = dataStore.data.map { it[showSpendingKey] ?: false }

    suspend fun setShowSpending(enabled: Boolean) {
        dataStore.edit { it[showSpendingKey] = enabled }
    }
}
