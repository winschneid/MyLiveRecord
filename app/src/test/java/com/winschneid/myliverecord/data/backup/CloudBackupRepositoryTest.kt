package com.winschneid.myliverecord.data.backup

import android.app.PendingIntent
import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.winschneid.myliverecord.data.auth.AuthOutcome
import com.winschneid.myliverecord.data.auth.DriveAuthorizer
import com.winschneid.myliverecord.data.auth.GoogleAccountRepository
import com.winschneid.myliverecord.domain.model.LiveRecord
import com.winschneid.myliverecord.domain.usecase.AddLiveRecordUseCase
import com.winschneid.myliverecord.domain.usecase.ExportLiveRecordsJsonUseCase
import com.winschneid.myliverecord.domain.usecase.GetLiveRecordsUseCase
import com.winschneid.myliverecord.domain.usecase.ImportLiveRecordsJsonUseCase
import com.winschneid.myliverecord.testutil.FakeLiveRecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import java.io.File

class CloudBackupRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeAuthorizer : DriveAuthorizer {
        val outcomes = ArrayDeque<AuthOutcome>()
        val invalidated = mutableListOf<String>()
        override suspend fun authorize(email: String): AuthOutcome =
            outcomes.removeFirstOrNull() ?: AuthOutcome.Token("tok")
        override fun resultFromIntent(data: Intent?): String = "tok"
        override suspend fun invalidate(accessToken: String) { invalidated += accessToken }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val transport = FakeHttpTransport()
    private val authorizer = FakeAuthorizer()
    private val localRepository = FakeLiveRecordRepository(
        listOf(LiveRecord(id = 1, artistNames = listOf("Ado"), venueName = "v", seatNumber = "", date = 1L))
    )
    private val accountRepository by lazy {
        GoogleAccountRepository(
            PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "test.preferences_pb") }
        )
    }
    private val repository by lazy {
        CloudBackupRepository(
            authorizer = authorizer,
            client = DriveAppDataClient(transport),
            exportLiveRecordsJson = ExportLiveRecordsJsonUseCase(GetLiveRecordsUseCase(localRepository)),
            importLiveRecordsJson = ImportLiveRecordsJsonUseCase(AddLiveRecordUseCase(localRepository)),
            accountRepository = accountRepository,
        )
    }

    private val fileJson = """{"id":"f1","name":"live_records_backup.json","modifiedTime":"2026-10-14T01:02:03Z"}"""
    private val backupJson = """{"version":1,"records":[{"artists":["YOASOBI"],"venue":"dome","seat":"","date":5,"memo":"","ticketPrice":null}]}"""

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `backup はファイルが無ければ作成し最終バックアップ日時を保存する`() = runBlocking {
        transport.enqueue(200, """{"files":[]}""")
        transport.enqueue(200, fileJson)

        val result = repository.backup("me@example.com")

        assertEquals(1, result.count)
        assertEquals(listOf("GET", "POST"), transport.requests.map { it.method })
        assertEquals(result.remote.modifiedAt, accountRepository.lastBackupAt.first())
    }

    @Test
    fun `backup は既存ファイルがあれば上書きする`() = runBlocking {
        transport.enqueue(200, """{"files":[$fileJson]}""")
        transport.enqueue(200, fileJson)

        repository.backup("me@example.com")

        assertEquals(listOf("GET", "PATCH"), transport.requests.map { it.method })
        assertTrue(transport.requests[1].url.contains("/files/f1?"))
    }

    @Test
    fun `401 なら一度だけトークンを捨てて取り直す`() = runBlocking {
        authorizer.outcomes.addLast(AuthOutcome.Token("stale"))
        authorizer.outcomes.addLast(AuthOutcome.Token("fresh"))
        transport.enqueue(401)
        transport.enqueue(200, """{"files":[]}""")

        assertNull(repository.fetchRemoteInfo("me@example.com"))

        assertEquals(listOf("stale"), authorizer.invalidated)
        assertEquals("Bearer fresh", transport.requests[1].headers["Authorization"])
    }

    @Test
    fun `401 が続けば Unauthorized のまま失敗する`() = runBlocking {
        transport.enqueue(401)
        transport.enqueue(401)

        assertThrows(DriveError.Unauthorized::class.java) {
            runBlocking { repository.fetchRemoteInfo("me@example.com") }
        }
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun `同意が必要なら ConsentRequiredException に PendingIntent が入る`() = runBlocking {
        val pendingIntent = mock(PendingIntent::class.java)
        authorizer.outcomes.addLast(AuthOutcome.NeedsConsent(pendingIntent))

        val e = assertThrows(ConsentRequiredException::class.java) {
            runBlocking { repository.fetchRemoteInfo("me@example.com") }
        }

        assertSame(pendingIntent, e.pendingIntent)
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun `fetchRestoreCandidate はダウンロードして解析するが書き込まない`() = runBlocking {
        transport.enqueue(200, """{"files":[$fileJson]}""")
        transport.enqueue(200, backupJson)

        val candidate = repository.fetchRestoreCandidate("me@example.com")

        assertNotNull(candidate)
        assertEquals("YOASOBI", candidate!!.records.single().artistNames.single())
        assertTrue(localRepository.added.isEmpty())
    }

    @Test
    fun `fetchRestoreCandidate はファイルが無ければ null`() = runBlocking {
        transport.enqueue(200, """{"files":[]}""")

        assertNull(repository.fetchRestoreCandidate("me@example.com"))
    }

    @Test
    fun `applyRestore は既存記録に追記する`() = runBlocking {
        transport.enqueue(200, """{"files":[$fileJson]}""")
        transport.enqueue(200, backupJson)
        val candidate = repository.fetchRestoreCandidate("me@example.com")!!

        val count = repository.applyRestore(candidate)

        assertEquals(1, count)
        assertEquals(2, localRepository.observeAllRecords().first().size)
    }
}
