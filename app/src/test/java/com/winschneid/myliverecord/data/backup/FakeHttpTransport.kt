package com.winschneid.myliverecord.data.backup

import java.io.IOException

/** 事前に積んだレスポンス（または IOException）を順に返し、受け取ったリクエストを記録する。 */
class FakeHttpTransport : HttpTransport {

    val requests = mutableListOf<HttpRequest>()
    private val queue = ArrayDeque<Result<HttpResponse>>()

    fun enqueue(code: Int, body: String = "") {
        queue.addLast(Result.success(HttpResponse(code, body.toByteArray())))
    }

    fun enqueueNetworkFailure() {
        queue.addLast(Result.failure(IOException("offline")))
    }

    override fun execute(request: HttpRequest): HttpResponse {
        requests += request
        val next = queue.removeFirstOrNull() ?: error("no response queued for ${request.method} ${request.url}")
        return next.getOrThrow()
    }
}
