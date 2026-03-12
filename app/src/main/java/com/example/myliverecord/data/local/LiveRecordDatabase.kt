package com.example.myliverecord.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myliverecord.data.local.dao.LiveRecordDao
import com.example.myliverecord.data.local.entity.LiveRecordEntity

@Database(
    entities = [LiveRecordEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class LiveRecordDatabase : RoomDatabase() {
    abstract fun liveRecordDao(): LiveRecordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE live_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        artist_names TEXT NOT NULL,
                        venue_name TEXT NOT NULL,
                        seat_number TEXT NOT NULL,
                        date INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO live_records_new (id, artist_names, venue_name, seat_number, date)
                    SELECT id, artist_name, venue_name, seat_number, date
                    FROM live_records
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE live_records")
                db.execSQL("ALTER TABLE live_records_new RENAME TO live_records")
            }
        }
    }
}
