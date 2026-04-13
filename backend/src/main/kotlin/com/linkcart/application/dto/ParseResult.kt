package com.linkcart.application.dto

import com.linkcart.domain.entity.Product

/**
 * 파싱 결과를 표현하는 sealed interface.
 *
 * 주의: Presentation 레이어에서 JSON 응답을 만들 때,
 * Success.product의 필드를 top-level로 펼쳐야 합니다 (nested "product" 키 사용 금지).
 * 프론트엔드 ParseResponse가 Product를 extends하는 flat 구조이기 때문입니다.
 */
sealed interface ParseResult {
    data class Success(
        val product: Product,
        val parserUsed: String,
        val fallbackUsed: Boolean = false,
    ) : ParseResult

    data class Partial(
        val fields: Map<String, Any>,
        val parserUsed: String,
        val fallbackUsed: Boolean = false,
    ) : ParseResult

    data class Failure(
        val reason: String,
        val parserUsed: String,
    ) : ParseResult
}
