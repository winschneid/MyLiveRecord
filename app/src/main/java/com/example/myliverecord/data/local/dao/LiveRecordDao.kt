package com.example.myliverecord.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myliverecord.data.local.entity.LiveRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveRecordDao {

    @Query("SELECT * FROM live_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<LiveRecordEntity>>

    @Insert
    suspend fun insert(record: LiveRecordEntity)

    @Query("SELECT COUNT(*) FROM live_records WHERE artist_name = :artistName")
    suspend fun getArtistCount(artistName: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM live_records
        WHERE CAST(strftime('%Y', datetime(date / 1000, 'unixepoch')) AS INTEGER) = :year
        """
    )
    suspend fun getYearCount(year: Int): Int
}
