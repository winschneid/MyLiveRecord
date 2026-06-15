package com.winschneid.myliverecord.ui.screens.history

import com.winschneid.myliverecord.domain.model.LiveRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HistorySectionsTest {

    private fun dateOf(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis

    private fun record(
        id: Long,
        date: Long,
        artists: List<String>,
        venue: String = "venue",
        title: String = "",
    ) = LiveRecord(
        id = id,
        title = title,
        artistNames = artists,
        venueName = venue,
        seatNumber = "",
        date = date,
    )

    // DAO と同じ日付降順で渡す
    private val records = listOf(
        record(3, dateOf(2025, 3, 10), listOf("YOASOBI"), venue = "東京ドーム"),
        record(2, dateOf(2025, 1, 20), listOf("Ado"), venue = "日本武道館", title = "心臓ツアー"),
        record(1, dateOf(2025, 1, 5), listOf("YOASOBI"), venue = "横浜アリーナ"),
    )

    @Test
    fun `年月ごとにセクション分けされ日付降順を維持する`() {
        val sections = buildHistorySections(records, query = "")

        assertEquals(listOf("2025年3月", "2025年1月"), sections.map { it.label })
        assertEquals(listOf(3L), sections[0].items.map { it.id })
        assertEquals(listOf(2L, 1L), sections[1].items.map { it.id })
    }

    @Test
    fun `アーティスト名・会場名・公演名で部分一致検索できる`() {
        assertEquals(
            listOf(3L, 1L),
            buildHistorySections(records, "yoasobi").flatMap { it.items }.map { it.id },
        )
        assertEquals(
            listOf(2L),
            buildHistorySections(records, "武道館").flatMap { it.items }.map { it.id },
        )
        assertEquals(
            listOf(2L),
            buildHistorySections(records, "心臓").flatMap { it.items }.map { it.id },
        )
        assertEquals(
            emptyList<HistorySection>(),
            buildHistorySections(records, "存在しない"),
        )
    }

    @Test
    fun `検索で絞り込んでもn回目はフィルタ前の全履歴基準のまま`() {
        val sections = buildHistorySections(records, "東京ドーム")

        val item = sections.single().items.single()
        assertEquals(3L, item.id)
        // id=1（1回目）はヒットしていないが、id=3 は通算2回目のまま
        assertEquals(2, item.artistVisitCounts.getValue("YOASOBI"))
    }
}
