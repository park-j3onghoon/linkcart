package com.linkcart.application.error

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

/**
 * AIP-193 / gRPC `google.rpc.Code` 부분집합. 새 의미가 필요하면 enum을 늘리지 말고
 * `ErrorDetail.reason`에 sub-reason을 넣는다 (google.rpc.ErrorInfo 패턴).
 *
 * 신뢰원천은 응답 본문의 `code` 문자열. HTTP status는 보조 매핑일 뿐, 클라이언트 분기는 `code`로.
 *
 * 참고: https://cloud.google.com/apis/design/errors
 */
enum class ErrorCode(val httpStatus: HttpStatus) {
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),

    /** vs [PERMISSION_DENIED]: 401은 "당신이 누군지 모름", 403은 "신원은 OK인데 권한 없음". */
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),

    /** 존재 여부 자체가 비밀이면 [NOT_FOUND]로 응답해 enumeration을 막는다 (ShareList :lookup이 이 패턴). */
    PERMISSION_DENIED(HttpStatus.FORBIDDEN),

    NOT_FOUND(HttpStatus.NOT_FOUND),

    ALREADY_EXISTS(HttpStatus.CONFLICT),

    /** vs [INVALID_ARGUMENT]: 요청 형식 자체의 오류가 아니라 시스템 상태가 작업을 거부할 때. */
    FAILED_PRECONDITION(HttpStatus.PRECONDITION_FAILED),

    /** `RetryInfo` detail에 retryDelay를 같이 보내면 클라이언트가 백오프 카운트다운 UI를 띄울 수 있다. */
    RESOURCE_EXHAUSTED(HttpStatus.TOO_MANY_REQUESTS),

    /** 클라이언트 메시지에 stack trace·내부 필드 절대 노출 금지. 원본 예외는 log.error로만 보존. */
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR),

    /** vs [INTERNAL]: 재시도하면 성공할 수 있음. 클라이언트가 지수 백오프로 자동 재시도해도 안전. */
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    companion object {
        fun fromHttpStatus(status: HttpStatusCode): ErrorCode {
            val direct = entries.firstOrNull { it.httpStatus.value() == status.value() }
            if (direct != null) return direct
            return when (status.value()) {
                // 502 Bad Gateway / 504 Gateway Timeout도 transient로 본다
                502, 504 -> UNAVAILABLE
                in 400..499 -> INVALID_ARGUMENT
                in 500..599 -> INTERNAL
                else -> INTERNAL
            }
        }
    }
}
