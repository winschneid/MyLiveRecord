package com.winschneid.myliverecord.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.winschneid.myliverecord.data.local.dao.LiveRecordDao
import com.winschneid.myliverecord.data.local.entity.ArtistEntity
import com.winschneid.myliverecord.data.local.entity.LiveRecordArtistCrossRef
import com.winschneid.myliverecord.data.local.entity.LiveRecordEntity

@Database(
    entities = [
        LiveRecordEntity::class,
        ArtistEntity::class,
        LiveRecordArtistCrossRef::class,
    ],
    version = 3,
    exportSchema = true,
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

        /**
         * v2 → v3: カンマ区切りだった artist_names を artists / live_record_artists に正規化し、
         * live_records に title / memo / ticket_price を追加する。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 旧テーブルを作り直す前に、分割対象のアーティスト文字列を読み出しておく
                data class OldRecord(val id: Long, val artistNames: List<String>)

                val oldRecords = mutableListOf<OldRecord>()
                db.query("SELECT id, artist_names FROM live_records").use { cursor ->
                    while (cursor.moveToNext()) {
                        val names = cursor.getString(1)
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        oldRecords += OldRecord(cursor.getLong(0), names)
                    }
                }

                db.execSQL(
                    """
                    CREATE TABLE live_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        venue_name TEXT NOT NULL,
                        seat_number TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        memo TEXT NOT NULL,
                        ticket_price INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO live_records_new (id, title, venue_name, seat_number, date, memo, ticket_price)
                    SELECT id, '', venue_name, seat_number, date, '', NULL
                    FROM live_records
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE live_records")
                db.execSQL("ALTER TABLE live_records_new RENAME TO live_records")

                db.execSQL(
                    """
                    CREATE TABLE artists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX index_artists_name ON artists (name)")
                db.execSQL(
                    """
                    CREATE TABLE live_record_artists (
                        record_id INTEGER NOT NULL,
                        artist_id INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        PRIMARY KEY(record_id, artist_id),
                        FOREIGN KEY(record_id) REFERENCES live_records(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(artist_id) REFERENCES artists(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX index_live_record_artists_artist_id ON live_record_artists (artist_id)")

                oldRecords.forEach { record ->
                    record.artistNames.forEachIndexed { position, name ->
                        db.execSQL("INSERT OR IGNORE INTO artists (name) VALUES (?)", arrayOf(name))
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO live_record_artists (record_id, artist_id, position)
                            VALUES (?, (SELECT id FROM artists WHERE name = ?), ?)
                            """.trimIndent(),
                            arrayOf(record.id, name, position),
                        )
                    }
                }
            }
        }
    }
}
