package com.winschneid.myliverecord.domain.transfer

import com.winschneid.myliverecord.domain.model.LiveRecord
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LiveRecordsJsonTest {

    private val records = listOf(
        LiveRecord(
            id = 1,
            title = "ツアー2026",
            artistNames = listOf("YOASOBI", "Ado"),
            venueName = "東京ドーム",
            seatNumber = "1塁側 A12",
            date = 1_780_000_000_000L,
            memo = "最高だった",
            ticketPrice = 9900,
        ),
        LiveRecord(
            id = 2,
            artistNames = listOf("Ado"),
            venueName = "日本武道館",
            seatNumber = "",
            date = 1_770_000_000_000L,
            ticketPrice = null,
        ),
    )

    @Test
    fun `encode して decode すると id 以外は元に戻る`() {
        val decoded = LiveRecordsJson.decode(LiveRecordsJson.encode(records))

        assertEquals(records.map { it.copy(id = 0) }, decoded)
    }

    @Test
    fun `encode は version を含む`() {
        val root = JSONObject(LiveRecordsJson.encode(records))

        assertEquals(LiveRecordsJson.FORMAT_VERSION, root.getInt("version"))
        assertEquals(2, root.getJSONArray("records").length())
    }

    @Test
    fun `アーティストが空の記録は decode で拒否される`() {
        val json = """{"version":1,"records":[{"artists":[" "],"venue":"v","date":1}]}"""

        assertThrows(IllegalArgumentException::class.java) { LiveRecordsJson.decode(json) }
    }

    @Test
    fun `records が無い JSON は decode で拒否される`() {
        assertThrows(Exception::class.java) { LiveRecordsJson.decode("""{"foo":1}""") }
    }
}
