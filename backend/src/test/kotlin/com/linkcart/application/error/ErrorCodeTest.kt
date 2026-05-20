package com.linkcart.application.error

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import kotlin.test.assertEquals

class ErrorCodeTest {

    @Test
    fun `fromHttpStatus matches enum directly when status is in the table`() {
        assertEquals(ErrorCode.NOT_FOUND, ErrorCode.fromHttpStatus(HttpStatus.NOT_FOUND))
        assertEquals(ErrorCode.UNAUTHENTICATED, ErrorCode.fromHttpStatus(HttpStatus.UNAUTHORIZED))
        assertEquals(ErrorCode.PERMISSION_DENIED, ErrorCode.fromHttpStatus(HttpStatus.FORBIDDEN))
        assertEquals(ErrorCode.ALREADY_EXISTS, ErrorCode.fromHttpStatus(HttpStatus.CONFLICT))
        assertEquals(ErrorCode.RESOURCE_EXHAUSTED, ErrorCode.fromHttpStatus(HttpStatus.TOO_MANY_REQUESTS))
        assertEquals(ErrorCode.UNAVAILABLE, ErrorCode.fromHttpStatus(HttpStatus.SERVICE_UNAVAILABLE))
    }

    @Test
    fun `502 and 504 fall back to UNAVAILABLE as transient gateway errors`() {
        assertEquals(ErrorCode.UNAVAILABLE, ErrorCode.fromHttpStatus(HttpStatus.BAD_GATEWAY))
        assertEquals(ErrorCode.UNAVAILABLE, ErrorCode.fromHttpStatus(HttpStatus.GATEWAY_TIMEOUT))
    }

    @Test
    fun `unknown 4xx maps to INVALID_ARGUMENT`() {
        assertEquals(ErrorCode.INVALID_ARGUMENT, ErrorCode.fromHttpStatus(HttpStatusCode.valueOf(418)))
    }

    @Test
    fun `unknown 5xx maps to INTERNAL`() {
        assertEquals(ErrorCode.INTERNAL, ErrorCode.fromHttpStatus(HttpStatusCode.valueOf(599)))
    }

    @Test
    fun `out-of-range status maps to INTERNAL fallback`() {
        assertEquals(ErrorCode.INTERNAL, ErrorCode.fromHttpStatus(HttpStatusCode.valueOf(200)))
        assertEquals(ErrorCode.INTERNAL, ErrorCode.fromHttpStatus(HttpStatusCode.valueOf(300)))
    }
}
