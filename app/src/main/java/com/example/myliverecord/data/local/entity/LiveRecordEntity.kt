package com.example.myliverecord.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_records")
data class LiveRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "artist_names")
    val artistNames: String, // カンマ区切りで複数アーティストを保存
    @ColumnInfo(name = "venue_name")
    val venueName: String,
    @ColumnInfo(name = "seat_number")
    val seatNumber: String,
    val date: Long,
)
