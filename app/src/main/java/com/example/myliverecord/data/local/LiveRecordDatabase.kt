package com.example.myliverecord.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myliverecord.data.local.dao.LiveRecordDao
import com.example.myliverecord.data.local.entity.LiveRecordEntity

@Database(
    entities = [LiveRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LiveRecordDatabase : RoomDatabase() {
    abstract fun liveRecordDao(): LiveRecordDao
}
