package com.winschneid.myliverecord.data.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/** Drive 上のバックアップファイルのメタデータ。 */
data class RemoteBackup(
    val fileId: String,
    /** Drive の modifiedTime（epoch millis） */
    val modifiedAt: Long,
    val sizeBytes: Long?,
)

sealed class DriveError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** 401: アクセストークン失効など */
    data object Unauthorized : DriveError("401 unauthorized")

    /** 403: Drive API 未有効化・スコープ不足など */
    data object Forbidden : DriveError("403 forbidden")

    /** オフライン・タイムアウトなど */
    class Network(cause: IOException) : DriveError("network error", cause)

    class Http(val code: Int, body: String) : DriveError("HTTP $code: ${body.take(200)}")
}

/**
 * Google Drive REST v3 の appDataFolder（ユーザーには見えないアプリ専用領域）を直接叩く。
 * 扱うのは単一ファイル [BACKUP_FILE_NAME] だけ。
 */
@Singleton
class DriveAppDataClient @Inject constructor(
    private val transport: HttpTransport,
) {

    /** バックアップファイルを探す。複数あれば最新を返す。無ければ null。 */
    fun findBackup(accessToken: String): RemoteBackup? {
        val query = URLEncoder.encode("name = '$BACKUP_FILE_NAME'", "UTF-8")
        val fields = URLEncoder.encode("files(id,name,modifiedTime,size)", "UTF-8")
        val response = execute(
            HttpRequest(
                method = "GET",
                url = "$API_BASE/drive/v3/files?spaces=appDataFolder&q=$query&fields=$fields",
                headers = authHeaders(accessToken),
            )
        )
        val files = JSONObject(response.bodyText).optJSONArray("files") ?: JSONArray()
        return (0 until files.length())
            .map { files.getJSONObject(it) }
            .filter { it.optString("name") == BACKUP_FILE_NAME }
            .map { it.toRemoteBackup() }
            .maxByOrNull { it.modifiedAt }
    }

    /** 新規作成（multipart: メタデータ + 本文）。 */
    fun create(accessToken: String, json: String): RemoteBackup {
        val metadata = JSONObject()
            .put("name", BACKUP_FILE_NAME)
            .put("mimeType", JSON_MIME)
            .put("parents", JSONArray().put("appDataFolder"))
            .toString()
        val body = buildString {
            append("--$MULTIPART_BOUNDARY\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata).append("\r\n")
            append("--$MULTIPART_BOUNDARY\r\n")
            append("Content-Type: $JSON_MIME\r\n\r\n")
            append(json).append("\r\n")
            append("--$MULTIPART_BOUNDARY--")
        }.toByteArray()
        val response = execute(
            HttpRequest(
                method = "POST",
                url = "$UPLOAD_BASE/upload/drive/v3/files?uploadType=multipart&fields=id,name,modifiedTime,size",
                headers = authHeaders(accessToken) +
                    ("Content-Type" to "multipart/related; boundary=$MULTIPART_BOUNDARY"),
                body = body,
            )
        )
        return JSONObject(response.bodyText).toRemoteBackup()
    }

    /** 既存ファイルの本文を置き換える。 */
    fun update(accessToken: String, fileId: String, json: String): RemoteBackup {
        val response = execute(
            HttpRequest(
                method = "PATCH",
                url = "$UPLOAD_BASE/upload/drive/v3/files/$fileId?uploadType=media&fields=id,name,modifiedTime,size",
                headers = authHeaders(accessToken) + ("Content-Type" to JSON_MIME),
                body = json.toByteArray(),
            )
        )
        return JSONObject(response.bodyText).toRemoteBackup()
    }

    fun download(accessToken: String, fileId: String): String {
        val response = execute(
            HttpRequest(
                method = "GET",
                url = "$API_BASE/drive/v3/files/$fileId?alt=media",
                headers = authHeaders(accessToken),
            )
        )
        return response.bodyText
    }

    private fun execute(request: HttpRequest): HttpResponse {
        val response = try {
            transport.execute(request)
        } catch (e: IOException) {
            throw DriveError.Network(e)
        }
        return when (response.code) {
            in 200..299 -> response
            401 -> throw DriveError.Unauthorized
            403 -> throw DriveError.Forbidden
            else -> throw DriveError.Http(response.code, response.bodyText)
        }
    }

    private fun authHeaders(accessToken: String) = mapOf(
        "Authorization" to "Bearer $accessToken",
        "Accept" to JSON_MIME,
    )

    private fun JSONObject.toRemoteBackup() = RemoteBackup(
        fileId = getString("id"),
        modifiedAt = parseRfc3339(optString("modifiedTime")),
        sizeBytes = if (has("size")) optString("size").toLongOrNull() else null,
    )

    companion object {
        const val BACKUP_FILE_NAME = "live_records_backup.json"
        private const val API_BASE = "https://www.googleapis.com"
        private const val UPLOAD_BASE = "https://www.googleapis.com"
        private const val JSON_MIME = "application/json"
        private const val MULTIPART_BOUNDARY = "myliverecord_backup_boundary"

        /** Drive の modifiedTime（例: 2026-10-14T01:02:03.456Z）。解釈できなければ 0。 */
        internal fun parseRfc3339(text: String): Long {
            if (text.isEmpty()) return 0L
            // 秒以下の桁数は環境で変わるので落としてから解釈する
            val normalized = text.replace(Regex("\\.\\d+"), "")
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return try {
                format.parse(normalized)?.time ?: 0L
            } catch (e: ParseException) {
                0L
            }
        }
    }
}
