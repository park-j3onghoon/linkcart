package com.linkcart.presentation.dto

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

/**
 * AIP-193 google.rpc.Code subset.
 * HTTP status는 권장 매핑이며, 응답 본문의 code(enum string)가 신뢰원천이다.
 */
enum class ErrorCode(val httpStatus: HttpStatus) {
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    ALREADY_EXISTS(HttpStatus.CONFLICT),
    FAILED_PRECONDITION(HttpStatus.PRECONDITION_FAILED),
    RESOURCE_EXHAUSTED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    companion object {
        fun fromHttpStatus(status: HttpStatusCode): ErrorCode {
            val direct = entries.firstOrNull { it.httpStatus.value() == status.value() }
            if (direct != null) return direct
            return when (status.value()) {
                502, 504 -> UNAVAILABLE
                in 400..499 -> INVALID_ARGUMENT
                in 500..599 -> INTERNAL
                else -> INTERNAL
            }
        }
    }
}
