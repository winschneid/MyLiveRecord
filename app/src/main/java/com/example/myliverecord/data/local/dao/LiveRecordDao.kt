package com.example.myliverecord.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myliverecord.data.local.entity.ArtistEntity
import com.example.myliverecord.data.local.entity.LiveRecordArtistCrossRef
import com.example.myliverecord.data.local.entity.LiveRecordEntity
import com.example.myliverecord.data.local.entity.LiveRecordRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveRecordDao {

    @Query(
        """
        SELECT lr.id, lr.title, lr.venue_name, lr.seat_number, lr.date, lr.memo, lr.ticket_price,
               a.name AS artist_name
        FROM live_records lr
        LEFT JOIN live_record_artists lra ON lra.record_id = lr.id
        LEFT JOIN artists a ON a.id = lra.artist_id
        ORDER BY lr.date DESC, lr.id DESC, lra.position ASC
        """
    )
    fun getAllRecordRows(): Flow<List<LiveRecordRow>>

    @Query(
        """
        SELECT lr.id, lr.title, lr.venue_name, lr.seat_number, lr.date, lr.memo, lr.ticket_price,
               a.name AS artist_name
        FROM live_records lr
        LEFT JOIN live_record_artists lra ON lra.record_id = lr.id
        LEFT JOIN artists a ON a.id = lra.artist_id
        WHERE lr.id = :id
        ORDER BY lra.position ASC
        """
    )
    suspend fun getRecordRowsById(id: Long): List<LiveRecordRow>

    @Insert
    suspend fun insertRecord(record: LiveRecordEntity): Long

    @Update
    suspend fun updateRecord(record: LiveRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity): Long

    @Query("SELECT id FROM artists WHERE name = :name")
    suspend fun getArtistIdByName(name: String): Long?

    @Insert
    suspend fun insertCrossRefs(refs: List<LiveRecordArtistCrossRef>)

    @Query("DELETE FROM live_record_artists WHERE record_id = :recordId")
    suspend fun deleteCrossRefsForRecord(recordId: Long)

    @Query("DELETE FROM live_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    /** どの記録からも参照されなくなったアーティストを削除（サジェスト汚染を防ぐ） */
    @Query("DELETE FROM artists WHERE id NOT IN (SELECT DISTINCT artist_id FROM live_record_artists)")
    suspend fun deleteOrphanArtists()

    @Query("SELECT name FROM artists ORDER BY name ASC")
    fun getAllArtistNames(): Flow<List<String>>

    @Query("SELECT DISTINCT venue_name FROM live_records ORDER BY venue_name ASC")
    fun getDistinctVenueNames(): Flow<List<String>>

    @Transaction
    suspend fun insertWithArtists(record: LiveRecordEntity, artistNames: List<String>) {
        val recordId = insertRecord(record)
        linkArtists(recordId, artistNames)
    }

    @Transaction
    suspend fun updateWithArtists(record: LiveRecordEntity, artistNames: List<String>) {
        updateRecord(record)
        deleteCrossRefsForRecord(record.id)
        linkArtists(record.id, artistNames)
        deleteOrphanArtists()
    }

    @Transaction
    suspend fun deleteWithCleanup(id: Long) {
        deleteRecordById(id)
        deleteOrphanArtists()
    }

    private suspend fun linkArtists(recordId: Long, artistNames: List<String>) {
        val refs = artistNames.mapIndexed { index, name ->
            val artistId = insertArtist(ArtistEntity(name = name)).takeIf { it != -1L }
                ?: requireNotNull(getArtistIdByName(name)) { "artist not found: $name" }
            LiveRecordArtistCrossRef(recordId = recordId, artistId = artistId, position = index)
        }
        insertCrossRefs(refs)
    }
}
