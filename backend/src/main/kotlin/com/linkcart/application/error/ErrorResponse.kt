package com.linkcart.application.error

/**
 * AIP-193 / `google.rpc.Status` 부분집합. 모든 4xx·5xx 응답 본문이 이 모양을 따른다.
 *
 * 세 필드의 역할 분담:
 * - [code]: 머신리더블 분기 키. 클라이언트는 이 값으로 if/switch 분기.
 * - [message]: 사람이 읽을 수 있는 안전한 디폴트. 우리는 한국어 displayable 톤으로 작성.
 *   클라이언트는 이걸 그대로 보여줘도 되고, 자체 카피 사전이 있으면 덮어써도 된다.
 * - [details]: 구조화된 추가 컨텍스트. 단순 에러엔 비어 있고, validation·rate limit 등 풍부한 UX가 필요한 케이스에 채워진다.
 *
 * 참고: https://cloud.google.com/apis/design/errors#error_model
 */
data class ErrorResponse(
    val code: ErrorCode,
    val message: String,
    val details: List<ErrorDetail>? = null,
)

/**
 * `google.rpc.Status.details`의 한 엔트리.
 *
 * [type]은 detail의 종류를 식별하는 문자열이며, 종류에 따라 어떤 필드가 채워지는지 다르다.
 * Google 표준 타입과 본 클래스 필드의 매핑:
 *
 * | type             | 채워지는 필드              | 용도                                              |
 * |------------------|-------------------------|--------------------------------------------------|
 * | `"BadRequest"`   | [fieldViolations]       | 검증 실패. 각 필드별 위반 사유 (INVALID_ARGUMENT와 함께) |
 * | `"ErrorInfo"`    | [reason], [metadata]    | 머신리더블 sub-reason + 컨텍스트 (e.g. quota 이름)    |
 * | `"RetryInfo"`    | [metadata] (retryDelay) | 클라이언트가 언제 재시도할지 (RESOURCE_EXHAUSTED와 함께) |
 * | `"Help"`         | [metadata] (links)      | 문서·고객지원 링크                                  |
 * | `"LocalizedMessage"` | [metadata] (locale, message) | i18n 메시지                                |
 *
 * 표준에 없는 사내 detail이 필요하면 새 type 문자열을 정의해 추가 가능 (예: `"FraudCheck"`).
 *
 * 참고: https://github.com/googleapis/googleapis/blob/master/google/rpc/error_details.proto
 */
data class ErrorDetail(
    /** Google `google.rpc.*` 메시지 이름. 클라이언트는 이걸로 detail 종류를 분기한다. */
    val type: String,
    /** `BadRequest` 타입에서 사용. 각 입력 필드의 위반 사유 리스트. */
    val fieldViolations: List<FieldViolation>? = null,
    /** `ErrorInfo` 타입에서 사용. 머신리더블 sub-reason (예: "QUOTA_EXCEEDED", "INVALID_TOKEN"). */
    val reason: String? = null,
    /** 자유 형식 컨텍스트. type에 따라 의미가 다르다 (RetryInfo의 retryDelay, Help의 link URL 등). */
    val metadata: Map<String, String>? = null,
)

/**
 * `google.rpc.BadRequest.FieldViolation`.
 * 어떤 입력 필드가 왜 잘못됐는지 알려준다.
 *
 * - [field]: 위반 필드의 JSON path. 중첩이면 dot 표기 (`"product.price.amount"`),
 *   배열 인덱스는 대괄호 (`"items[0].sourceUrl"`). 클라이언트는 이 path로 해당 input에
 *   빨간 테두리·에러 텍스트를 띄울 수 있다.
 * - [description]: 위반 사유 (예: "비어 있을 수 없습니다", "100자 이하여야 합니다").
 *   클라이언트가 자체 카피 매핑이 있으면 덮어쓰고, 없으면 그대로 표시한다.
 */
data class FieldViolation(
    val field: String,
    val description: String,
)
