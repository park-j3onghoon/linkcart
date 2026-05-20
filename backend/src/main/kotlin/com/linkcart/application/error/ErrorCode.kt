package com.linkcart.application.error

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

/**
 * AIP-193 / gRPC `google.rpc.Code`의 부분집합.
 *
 * 17개 표준 코드 중 우리 도메인에서 실제로 발생하는 9개만 노출한다.
 * 새 의미가 필요하면 enum을 추가하지 말고 [ErrorDetail.reason]에 sub-reason을 넣는 것이 표준 (google.rpc.ErrorInfo 패턴).
 *
 * 신뢰원천은 응답 본문의 `code` 문자열이며, HTTP status는 보조적 호환 매핑이다.
 * 클라이언트가 분기 처리할 때는 HTTP status가 아니라 이 `code`로 분기한다.
 *
 * 참고: https://cloud.google.com/apis/design/errors, https://grpc.io/docs/guides/status-codes/
 */
enum class ErrorCode(val httpStatus: HttpStatus) {
    /**
     * 클라이언트가 요청 자체를 잘못 보냄. 시스템 상태와 무관하게 항상 잘못된 요청.
     *
     * 사용 예: 빈 URL, 음수 pageSize, 형식 안 맞는 enum 값, 파싱 실패한 page_token
     *
     * vs [FAILED_PRECONDITION]: 요청은 형식적으로 OK인데 시스템 상태가 안 맞을 때 쓴다.
     * (예: "삭제된 리소스를 수정 시도" 같은 상태 의존 에러)
     */
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),

    /**
     * 인증 정보(자격 증명)가 없거나 유효하지 않음. "당신이 누군지 모르겠음".
     *
     * 사용 예: Authorization 헤더 누락, 만료된 access token, 유효하지 않은 refresh token
     *
     * vs [PERMISSION_DENIED]: 신원은 확인됐는데 권한이 없는 경우.
     * 즉 401은 "로그인하세요", 403은 "당신은 로그인됐지만 이 리소스에 접근 불가".
     */
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),

    /**
     * 인증은 됐으나 해당 리소스/작업에 대한 권한 없음.
     *
     * 사용 예: 다른 사용자의 ShareList 삭제 시도, 관리자 전용 API 일반 사용자 호출
     *
     * 보안 팁: 존재 여부 자체가 secret이면 [NOT_FOUND]를 반환해 enumeration을 막는다.
     * (linkcart ShareList :lookup은 token이 secret이라 NOT_FOUND로 통합)
     */
    PERMISSION_DENIED(HttpStatus.FORBIDDEN),

    /**
     * 요청한 리소스가 존재하지 않거나 호출자에게 보이지 않음.
     *
     * 사용 예: 잘못된 ShareList token, 삭제된 UserProduct id, 존재하지 않는 user
     *
     * vs [PERMISSION_DENIED]: 존재 여부 자체가 비밀이라면 NOT_FOUND로 통일해
     * "리소스가 있는데 권한이 없다"는 정보 누설을 방지한다 (timing attack까지 신경 쓰면 추가 mitigation 필요).
     */
    NOT_FOUND(HttpStatus.NOT_FOUND),

    /**
     * 생성하려는 리소스가 이미 존재함. 멱등 키 충돌 / UNIQUE 제약 위반.
     *
     * 사용 예: 같은 source URL로 UserProduct 중복 저장, 이미 가입된 이메일로 회원가입
     *
     * 클라이언트는 이걸 보고 "다시 보낼 필요 없음, 기존 리소스 조회 후 사용"으로 처리한다.
     */
    ALREADY_EXISTS(HttpStatus.CONFLICT),

    /**
     * 요청은 형식적으로 valid하지만 시스템 상태가 작업을 허용하지 않음.
     *
     * 사용 예: "빈 productIds로 ShareList 생성", "이미 만료된 token으로 refresh", "필수 설정 미완료 상태에서 결제 시도"
     *
     * vs [INVALID_ARGUMENT]: 요청 자체의 형식 오류는 INVALID_ARGUMENT. 형식은 OK인데 상태 충돌이면 FAILED_PRECONDITION.
     * 판단 기준: "같은 요청을 다른 시점에 보내면 성공할 수 있는가?" → 예: FAILED_PRECONDITION. 아니오: INVALID_ARGUMENT.
     */
    FAILED_PRECONDITION(HttpStatus.PRECONDITION_FAILED),

    /**
     * 쿼터·레이트리밋 초과. 일정 시간 뒤 재시도하면 성공할 수 있음.
     *
     * 사용 예: parse rate limit (30 req/IP), :lookup 브루트포스 시도, 일일 호출량 초과
     *
     * `details`에 `RetryInfo { retryDelay: "30s" }`를 같이 보내 클라이언트가 백오프 카운트다운을 띄울 수 있게 한다.
     */
    RESOURCE_EXHAUSTED(HttpStatus.TOO_MANY_REQUESTS),

    /**
     * 서버 내부 오류. 불변식이 깨졌거나 예상치 못한 예외가 컨트롤러 밖으로 새어 나옴.
     *
     * 클라이언트 메시지에는 절대 stack trace·내부 필드·SQL을 노출하지 않고 고정 문구만 반환한다.
     * 원본 예외는 서버 로그(`log.error`)에만 보존한다.
     *
     * 이걸 자주 본다면 도메인 예외로 끌어올리거나 입력 검증을 추가해야 한다.
     */
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR),

    /**
     * 일시적으로 서비스를 사용할 수 없음. 외부 의존성 실패가 흔한 원인.
     *
     * 사용 예: 쿠팡/11번가 API 타임아웃, 이미지 프록시 upstream 실패, DB 일시 단절
     *
     * vs [INTERNAL]: INTERNAL은 "재시도해도 같은 결과"이고 UNAVAILABLE은 "잠시 후 재시도하면 성공할 수 있음".
     * 클라이언트는 UNAVAILABLE에 한해 지수 백오프로 자동 재시도해도 안전.
     */
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    companion object {
        /**
         * 알려지지 않은 HTTP status를 받았을 때(예: `ResponseStatusException` 우회 경로) 안전한 fallback 매핑.
         * 우선 enum 직접 매칭을 시도하고, 없으면 status 대역으로 추정.
         */
        fun fromHttpStatus(status: HttpStatusCode): ErrorCode {
            val direct = entries.firstOrNull { it.httpStatus.value() == status.value() }
            if (direct != null) return direct
            return when (status.value()) {
                502, 504 -> UNAVAILABLE // Bad Gateway / Gateway Timeout도 일시적 외부 의존성 실패로 본다
                in 400..499 -> INVALID_ARGUMENT
                in 500..599 -> INTERNAL
                else -> INTERNAL
            }
        }
    }
}
