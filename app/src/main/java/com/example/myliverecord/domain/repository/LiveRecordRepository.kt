package com.example.myliverecord.domain.repository

import com.example.myliverecord.domain.model.LiveRecord
import kotlinx.coroutines.flow.Flow

interface LiveRecordRepository {
    fun observeAllRecords(): Flow<List<LiveRecord>>
    suspend fun getRecordById(id: Long): LiveRecord?
    suspend fun addRecord(record: LiveRecord)
    suspend fun updateRecord(record: LiveRecord)
    suspend fun deleteRecord(id: Long)
    fun observeDistinctArtistNames(): Flow<List<String>>
    fun observeDistinctVenueNames(): Flow<List<String>>
}
