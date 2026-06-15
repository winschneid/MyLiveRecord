package com.winschneid.myliverecord.ui.screens.history

import com.winschneid.myliverecord.domain.model.LiveRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class VisitCountsTest {

    private fun record(id: Long, date: Long, vararg artists: String) = LiveRecord(
        id = id,
        artistNames = artists.toList(),
        venueName = "venue",
        seatNumber = "",
        date = date,
    )

    @Test
    fun `同一アーティストは日付昇順に1から連番が振られる`() {
        val records = listOf(
            record(id = 3, date = 3000, "YOASOBI"),
            record(id = 1, date = 1000, "YOASOBI"),
            record(id = 2, date = 2000, "YOASOBI"),
        )

        val counts = computeVisitCounts(records)

        assertEquals(1, counts.getValue(1).getValue("YOASOBI"))
        assertEquals(2, counts.getValue(2).getValue("YOASOBI"))
        assertEquals(3, counts.getValue(3).getValue("YOASOBI"))
    }

    @Test
    fun `フェス（複数アーティスト）は出演者ごとに独立してカウントされる`() {
        val records = listOf(
            record(id = 1, date = 1000, "YOASOBI"),
            record(id = 2, date = 2000, "YOASOBI", "King Gnu"),
        )

        val counts = computeVisitCounts(records)

        assertEquals(mapOf("YOASOBI" to 1), counts.getValue(1))
        assertEquals(mapOf("YOASOBI" to 2, "King Gnu" to 1), counts.getValue(2))
    }

    @Test
    fun `空リストは空マップを返す`() {
        assertEquals(emptyMap<Long, Map<String, Int>>(), computeVisitCounts(emptyList()))
    }
}
