package com.winschneid.myliverecord.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveAppDataClientTest {

    private val transport = FakeHttpTransport()
    private val client = DriveAppDataClient(transport)

    private val fileJson = """{"id":"f1","name":"live_records_backup.json","modifiedTime":"2026-10-14T01:02:03.456Z","size":"2048"}"""

    @Test
    fun `findBackup は appDataFolder を Bearer 付きで検索し最新を返す`() {
        transport.enqueue(
            200,
            """{"files":[
                {"id":"old","name":"live_records_backup.json","modifiedTime":"2025-01-01T00:00:00.000Z"},
                $fileJson
            ]}""",
        )

        val remote = client.findBackup("tok")!!

        val request = transport.requests.single()
        assertEquals("GET", request.method)
        assertTrue(request.url.startsWith("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder"))
        assertEquals("Bearer tok", request.headers["Authorization"])
        assertEquals("f1", remote.fileId)
        assertEquals(2048L, remote.sizeBytes)
        assertEquals(DriveAppDataClient.parseRfc3339("2026-10-14T01:02:03Z"), remote.modifiedAt)
    }

    @Test
    fun `findBackup はファイルが無ければ null`() {
        transport.enqueue(200, """{"files":[]}""")

        assertNull(client.findBackup("tok"))
    }

    @Test
    fun `create は multipart でメタデータと本文を送る`() {
        transport.enqueue(200, fileJson)

        client.create("tok", """{"version":1,"records":[]}""")

        val request = transport.requests.single()
        assertEquals("POST", request.method)
        assertTrue(request.url.startsWith("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"))
        assertTrue(request.headers.getValue("Content-Type").startsWith("multipart/related; boundary="))
        val body = request.body!!.decodeToString()
        assertTrue(body.contains("\"parents\":[\"appDataFolder\"]"))
        assertTrue(body.contains("\"name\":\"live_records_backup.json\""))
        assertTrue(body.contains("""{"version":1,"records":[]}"""))
    }

    @Test
    fun `update は PATCH media で本文を置き換える`() {
        transport.enqueue(200, fileJson)

        client.update("tok", "f1", "{}")

        val request = transport.requests.single()
        assertEquals("PATCH", request.method)
        assertTrue(request.url.startsWith("https://www.googleapis.com/upload/drive/v3/files/f1?uploadType=media"))
        assertEquals("{}", request.body!!.decodeToString())
    }

    @Test
    fun `download は alt=media で本文を返す`() {
        transport.enqueue(200, """{"version":1}""")

        val text = client.download("tok", "f1")

        assertEquals("""{"version":1}""", text)
        assertEquals("https://www.googleapis.com/drive/v3/files/f1?alt=media", transport.requests.single().url)
    }

    @Test
    fun `HTTP エラーは DriveError に写像される`() {
        transport.enqueue(401)
        assertThrows(DriveError.Unauthorized::class.java) { client.findBackup("tok") }

        transport.enqueue(403)
        assertThrows(DriveError.Forbidden::class.java) { client.findBackup("tok") }

        transport.enqueue(500, "boom")
        val http = assertThrows(DriveError.Http::class.java) { client.findBackup("tok") }
        assertEquals(500, http.code)

        transport.enqueueNetworkFailure()
        assertThrows(DriveError.Network::class.java) { client.findBackup("tok") }
    }

    @Test
    fun `modifiedTime は小数秒の有無によらず解釈できる`() {
        val withMillis = DriveAppDataClient.parseRfc3339("2026-10-14T01:02:03.456Z")
        val withoutMillis = DriveAppDataClient.parseRfc3339("2026-10-14T01:02:03Z")

        assertEquals(withoutMillis, withMillis)
        assertEquals(1_791_939_723_000L, withoutMillis)
        assertEquals(0L, DriveAppDataClient.parseRfc3339(""))
    }
}
