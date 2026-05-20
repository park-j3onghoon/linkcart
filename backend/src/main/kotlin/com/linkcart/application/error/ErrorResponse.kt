package com.linkcart.application.error

/**
 * AIP-193 / `google.rpc.Status` 부분집합. message는 한국어 displayable 디폴트로 작성하고,
 * 클라이언트는 자체 카피 사전이 있으면 덮어쓰는 fallback으로 사용한다.
 *
 * 참고: https://cloud.google.com/apis/design/errors#error_model
 */
data class ErrorResponse(
    val code: ErrorCode,
    val message: String,
    val details: List<ErrorDetail>? = null,
)

/**
 * `google.rpc.Status.details`의 한 엔트리. type별로 채워지는 필드가 다르다:
 *
 * | type                 | 필드                       |
 * |----------------------|---------------------------|
 * | `"BadRequest"`       | fieldViolations           |
 * | `"ErrorInfo"`        | reason, metadata          |
 * | `"RetryInfo"`        | metadata.retryDelay       |
 * | `"Help"`             | metadata.links            |
 * | `"LocalizedMessage"` | metadata.locale·message   |
 *
 * 표준에 없는 detail이 필요하면 새 type 문자열을 정의해 확장 가능 (예: `"FraudCheck"`).
 *
 * 참고: https://github.com/googleapis/googleapis/blob/master/google/rpc/error_details.proto
 */
data class ErrorDetail(
    val type: String,
    val fieldViolations: List<FieldViolation>? = null,
    val reason: String? = null,
    val metadata: Map<String, String>? = null,
)

/**
 * field는 JSON path (중첩은 dot `"product.price.amount"`, 배열은 대괄호 `"items[0].sourceUrl"`).
 * 클라이언트가 이 path로 해당 input에 에러 표시를 매핑한다.
 */
data class FieldViolation(
    val field: String,
    val description: String,
)
