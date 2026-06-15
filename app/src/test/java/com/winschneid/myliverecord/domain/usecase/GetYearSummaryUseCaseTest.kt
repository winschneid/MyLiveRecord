package com.winschneid.myliverecord.domain.usecase

import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.repository.LiveRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GetYearSummaryUseCaseTest {

    private class FakeRepository(private val records: List<LiveRecord>) : LiveRecordRepository {
        override fun observeAllRecords(): Flow<List<LiveRecord>> = flowOf(records)
        override suspend fun getRecordById(id: Long): LiveRecord? = null
        override suspend fun addRecord(record: LiveRecord) = Unit
        override suspend fun updateRecord(record: LiveRecord) = Unit
        override suspend fun deleteRecord(id: Long) = Unit
        override fun observeDistinctArtistNames(): Flow<List<String>> = flowOf(emptyList())
        override fun observeDistinctVenueNames(): Flow<List<String>> = flowOf(emptyList())
    }

    /** タイムゾーン境界の影響を避けるため年の中央（7月1日）の epoch millis を使う */
    private fun midYear(year: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, Calendar.JULY, 1)
        }.timeInMillis

    private fun record(
        id: Long,
        year: Int,
        artists: List<String>,
        price: Long? = null,
        venue: String = "venue",
    ) = LiveRecord(
        id = id,
        artistNames = artists,
        venueName = venue,
        seatNumber = "",
        date = midYear(year),
        ticketPrice = price,
    )

    @Test
    fun `年ごとにグループ化され新しい年が先頭になる`() = runBlocking {
        val useCase = GetYearSummaryUseCase(
            FakeRepository(
                listOf(
                    record(1, 2024, listOf("YOASOBI")),
                    record(2, 2025, listOf("Ado")),
                    record(3, 2024, listOf("YOASOBI")),
                )
            )
        )

        val summaries = useCase().first()

        assertEquals(listOf(2025, 2024), summaries.map { it.year })
        assertEquals(1, summaries[0].totalCount)
        assertEquals(2, summaries[1].totalCount)
    }

    @Test
    fun `アーティストは回数の多い順に並ぶ`() = runBlocking {
        val useCase = GetYearSummaryUseCase(
            FakeRepository(
                listOf(
                    record(1, 2024, listOf("Ado")),
                    record(2, 2024, listOf("YOASOBI", "Ado")),
                    record(3, 2024, listOf("YOASOBI")),
                    record(4, 2024, listOf("Ado")),
                )
            )
        )

        val artists = useCase().first().single().artists

        assertEquals("Ado", artists[0].artistName)
        assertEquals(3, artists[0].count)
        assertEquals("YOASOBI", artists[1].artistName)
        assertEquals(2, artists[1].count)
    }

    @Test
    fun `会場は回数の多い順に並ぶ`() = runBlocking {
        val useCase = GetYearSummaryUseCase(
            FakeRepository(
                listOf(
                    record(1, 2024, listOf("Ado"), venue = "東京ドーム"),
                    record(2, 2024, listOf("Ado"), venue = "日本武道館"),
                    record(3, 2024, listOf("Ado"), venue = "東京ドーム"),
                )
            )
        )

        val venues = useCase().first().single().venues

        assertEquals("東京ドーム", venues[0].venueName)
        assertEquals(2, venues[0].count)
        assertEquals("日本武道館", venues[1].venueName)
        assertEquals(1, venues[1].count)
    }

    @Test
    fun `チケット代は年ごとに合計され未入力はゼロ扱い`() = runBlocking {
        val useCase = GetYearSummaryUseCase(
            FakeRepository(
                listOf(
                    record(1, 2024, listOf("YOASOBI"), price = 9900),
                    record(2, 2024, listOf("Ado"), price = null),
                    record(3, 2024, listOf("Ado"), price = 12000),
                    record(4, 2025, listOf("Ado"), price = 8000),
                )
            )
        )

        val summaries = useCase().first()

        assertEquals(8000L, summaries.first { it.year == 2025 }.totalSpend)
        assertEquals(21900L, summaries.first { it.year == 2024 }.totalSpend)
    }
}
