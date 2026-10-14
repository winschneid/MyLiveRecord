package com.winschneid.myliverecord.domain.usecase

import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.testutil.FakeLiveRecordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportLiveRecordsJsonUseCaseTest {

    private fun record(id: Long, artist: String) = LiveRecord(
        id = id,
        artistNames = listOf(artist),
        venueName = "venue",
        seatNumber = "",
        date = 1_700_000_000_000L + id,
    )

    @Test
    fun `export は全件を書き出し件数を返す`() = runBlocking {
        val repository = FakeLiveRecordRepository(listOf(record(1, "Ado"), record(2, "YOASOBI")))
        val export = ExportLiveRecordsJsonUseCase(GetLiveRecordsUseCase(repository))

        val result = export()

        assertEquals(2, result.count)
        assertTrue(result.json.contains("YOASOBI"))
    }

    @Test
    fun `import は既存記録を消さずに追記する`() = runBlocking {
        val source = FakeLiveRecordRepository(listOf(record(1, "Ado"), record(2, "YOASOBI")))
        val json = ExportLiveRecordsJsonUseCase(GetLiveRecordsUseCase(source))().json

        val target = FakeLiveRecordRepository(listOf(record(10, "既存")))
        val import = ImportLiveRecordsJsonUseCase(AddLiveRecordUseCase(target))
        val count = import(json)

        assertEquals(2, count)
        assertEquals(
            listOf("既存", "Ado", "YOASOBI"),
            target.observeAllRecords().first().map { it.artistNames.single() },
        )
    }

    @Test
    fun `parse は書き込まない`() = runBlocking {
        val target = FakeLiveRecordRepository()
        val import = ImportLiveRecordsJsonUseCase(AddLiveRecordUseCase(target))
        val json = ExportLiveRecordsJsonUseCase(GetLiveRecordsUseCase(FakeLiveRecordRepository(listOf(record(1, "Ado")))))().json

        val parsed = import.parse(json)

        assertEquals(1, parsed.size)
        assertTrue(target.added.isEmpty())
    }

    @Test
    fun `不正な JSON は例外になり何も書き込まれない`() = runBlocking {
        val target = FakeLiveRecordRepository()
        val import = ImportLiveRecordsJsonUseCase(AddLiveRecordUseCase(target))

        assertThrows(Exception::class.java) { runBlocking { import("not json") } }
        assertTrue(target.added.isEmpty())
    }
}
