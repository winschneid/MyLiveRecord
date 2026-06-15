package com.winschneid.myliverecord.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "live_records")
data class LiveRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "venue_name")
    val venueName: String,
    @ColumnInfo(name = "seat_number")
    val seatNumber: String,
    val date: Long,
    val memo: String,
    @ColumnInfo(name = "ticket_price")
    val ticketPrice: Long?,
)

@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = true)],
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "live_record_artists",
    primaryKeys = ["record_id", "artist_id"],
    foreignKeys = [
        ForeignKey(
            entity = LiveRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("artist_id")],
)
data class LiveRecordArtistCrossRef(
    @ColumnInfo(name = "record_id")
    val recordId: Long,
    @ColumnInfo(name = "artist_id")
    val artistId: Long,
    // 出演者の表示順（入力順を保持する）
    val position: Int,
)

/** live_records と artists を JOIN したフラット行。repository 側で record 単位にまとめる */
data class LiveRecordRow(
    val id: Long,
    val title: String,
    @ColumnInfo(name = "venue_name")
    val venueName: String,
    @ColumnInfo(name = "seat_number")
    val seatNumber: String,
    val date: Long,
    val memo: String,
    @ColumnInfo(name = "ticket_price")
    val ticketPrice: Long?,
    @ColumnInfo(name = "artist_name")
    val artistName: String?,
)
