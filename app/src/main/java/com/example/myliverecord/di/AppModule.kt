package com.example.myliverecord.di

import android.content.Context
import androidx.room.Room
import com.example.myliverecord.data.local.LiveRecordDatabase
import com.example.myliverecord.data.local.dao.LiveRecordDao
import com.example.myliverecord.data.repository.LiveRecordRepositoryImpl
import com.example.myliverecord.domain.repository.LiveRecordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LiveRecordDatabase =
        Room.databaseBuilder(
            context,
            LiveRecordDatabase::class.java,
            "live_record_db",
        ).build()

    @Provides
    @Singleton
    fun provideLiveRecordDao(db: LiveRecordDatabase): LiveRecordDao = db.liveRecordDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLiveRecordRepository(
        impl: LiveRecordRepositoryImpl,
    ): LiveRecordRepository
}
