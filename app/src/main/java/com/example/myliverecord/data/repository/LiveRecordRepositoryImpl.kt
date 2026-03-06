package com.example.myliverecord.data.repository

import com.example.myliverecord.data.local.dao.LiveRecordDao
import com.example.myliverecord.data.local.entity.LiveRecordEntity
import com.example.myliverecord.domain.model.LiveRecord
import com.example.myliverecord.domain.repository.LiveRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LiveRecordRepositoryImpl @Inject constructor(
    private val dao: LiveRecordDao,
) : LiveRecordRepository {

    override fun observeAllRecords(): Flow<List<LiveRecord>> =
        dao.getAllRecords().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addRecord(record: LiveRecord) {
        dao.insert(record.toEntity())
    }

    private fun LiveRecordEntity.toDomain() = LiveRecord(
        id = id,
        artistName = artistName,
        venueName = venueName,
        seatNumber = seatNumber,
        date = date,
    )

    private fun LiveRecord.toEntity() = LiveRecordEntity(
        id = id,
        artistName = artistName,
        venueName = venueName,
        seatNumber = seatNumber,
        date = date,
    )
}
