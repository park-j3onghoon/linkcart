package com.linkcart.application.image.port

import org.springframework.http.MediaType

interface ImageFetcher {
    /** [FetchFailed]를 던질 수 있다. application은 어떤 HTTP 라이브러리를 쓰는지 알지 못한다. */
    fun fetch(url: String): Fetched

    data class Fetched(val bytes: ByteArray, val contentType: MediaType?)

    class FetchFailed(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}
