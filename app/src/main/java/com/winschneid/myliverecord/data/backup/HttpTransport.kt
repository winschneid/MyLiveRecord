package com.winschneid.myliverecord.data.backup

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
)

data class HttpResponse(
    val code: Int,
    val body: ByteArray,
) {
    val bodyText: String get() = body.decodeToString()
}

/** Drive REST を叩く最小限の HTTP 層。テストでは差し替える。 */
interface HttpTransport {
    /** 接続・読み取り失敗は IOException。HTTP エラーはレスポンスとして返す。 */
    @Throws(IOException::class)
    fun execute(request: HttpRequest): HttpResponse
}

class HttpUrlConnectionTransport @Inject constructor() : HttpTransport {

    override fun execute(request: HttpRequest): HttpResponse {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            // HttpURLConnection は PATCH を受け付けないので Google API のメソッド上書きヘッダを使う
            if (request.method == "PATCH") {
                connection.requestMethod = "POST"
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                connection.requestMethod = request.method
            }
            request.headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            request.body?.let { body ->
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code >= 400) connection.errorStream else connection.inputStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            return HttpResponse(code, bytes)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 30_000
    }
}
