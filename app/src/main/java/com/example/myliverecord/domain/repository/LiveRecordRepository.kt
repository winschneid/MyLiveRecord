package com.example.myliverecord.domain.repository

import com.example.myliverecord.domain.model.LiveRecord
import kotlinx.coroutines.flow.Flow

interface LiveRecordRepository {
    fun observeAllRecords(): Flow<List<LiveRecord>>
    suspend fun addRecord(record: LiveRecord)
}
