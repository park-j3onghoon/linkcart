package com.linkcart.application.error

/**
 * AIP-193 google.rpc.Status subset.
 * - code: ErrorCode enum (string으로 직렬화)
 * - message: 사용자에게 노출 가능한 메시지
 * - details: 추가 컨텍스트 (필드 검증 위반, 재시도 정보 등)
 */
data class ErrorResponse(
    val code: ErrorCode,
    val message: String,
    val details: List<ErrorDetail>? = null,
)

data class ErrorDetail(
    val type: String,
    val fieldViolations: List<FieldViolation>? = null,
    val reason: String? = null,
    val metadata: Map<String, String>? = null,
)

data class FieldViolation(
    val field: String,
    val description: String,
)
