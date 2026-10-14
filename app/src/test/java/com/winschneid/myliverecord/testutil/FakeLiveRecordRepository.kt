package com.winschneid.myliverecord.testutil

import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.repository.LiveRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** インメモリのリポジトリ。追加した記録は観測できる。 */
class FakeLiveRecordRepository(initial: List<LiveRecord> = emptyList()) : LiveRecordRepository {

    private val records = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val added = mutableListOf<LiveRecord>()

    override fun observeAllRecords(): Flow<List<LiveRecord>> = records

    override suspend fun getRecordById(id: Long): LiveRecord? = records.value.firstOrNull { it.id == id }

    override suspend fun addRecord(record: LiveRecord) {
        added += record
        records.value = records.value + record.copy(id = nextId++)
    }

    override suspend fun updateRecord(record: LiveRecord) {
        records.value = records.value.map { if (it.id == record.id) record else it }
    }

    override suspend fun deleteRecord(id: Long) {
        records.value = records.value.filterNot { it.id == id }
    }

    override fun observeDistinctArtistNames(): Flow<List<String>> =
        records.map { list -> list.flatMap { it.artistNames }.distinct() }

    override fun observeDistinctVenueNames(): Flow<List<String>> =
        records.map { list -> list.map { it.venueName }.distinct() }
}
