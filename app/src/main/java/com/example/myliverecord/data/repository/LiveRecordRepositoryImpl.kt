package com.example.myliverecord.data.repository

import com.example.myliverecord.data.local.dao.LiveRecordDao
import com.example.myliverecord.data.local.entity.LiveRecordEntity
import com.example.myliverecord.data.local.entity.LiveRecordRow
import com.example.myliverecord.domain.model.LiveRecord
import com.example.myliverecord.domain.repository.LiveRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LiveRecordRepositoryImpl @Inject constructor(
    private val dao: LiveRecordDao,
) : LiveRecordRepository {

    override fun observeAllRecords(): Flow<List<LiveRecord>> =
        dao.getAllRecordRows().map { rows -> rows.toDomainList() }

    override suspend fun getRecordById(id: Long): LiveRecord? =
        dao.getRecordRowsById(id).toDomainList().firstOrNull()

    override suspend fun addRecord(record: LiveRecord) {
        dao.insertWithArtists(record.toEntity(), record.artistNames)
    }

    override suspend fun updateRecord(record: LiveRecord) {
        dao.updateWithArtists(record.toEntity(), record.artistNames)
    }

    override suspend fun deleteRecord(id: Long) {
        dao.deleteWithCleanup(id)
    }

    override fun observeDistinctArtistNames(): Flow<List<String>> = dao.getAllArtistNames()

    override fun observeDistinctVenueNames(): Flow<List<String>> = dao.getDistinctVenueNames()

    /** JOIN 結果のフラット行を record 単位にまとめる。行順（日付降順・position 昇順）は groupBy で保持される */
    private fun List<LiveRecordRow>.toDomainList(): List<LiveRecord> =
        groupBy { it.id }.map { (id, rows) ->
            val first = rows.first()
            LiveRecord(
                id = id,
                title = first.title,
                artistNames = rows.mapNotNull { it.artistName },
                venueName = first.venueName,
                seatNumber = first.seatNumber,
                date = first.date,
                memo = first.memo,
                ticketPrice = first.ticketPrice,
            )
        }

    private fun LiveRecord.toEntity() = LiveRecordEntity(
        id = id,
        title = title,
        venueName = venueName,
        seatNumber = seatNumber,
        date = date,
        memo = memo,
        ticketPrice = ticketPrice,
    )
}
