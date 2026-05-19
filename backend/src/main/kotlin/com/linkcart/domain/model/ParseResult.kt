package com.linkcart.domain.model

import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money

/**
 * 파싱 결과를 표현하는 sealed interface.
 */
sealed interface ParseResult {
    data class Success(
        val product: Product,
        val parserUsed: ParserName,
        val fallbackUsed: Boolean = false,
    ) : ParseResult

    /**
     * 일부 필드만 추출에 성공한 경우. 어떤 필드가 채워질 수 있는지 명시한다.
     * (이전에는 Map<String, Any>로 표현했으나 타입 캐스팅 회피와 의도 명확화를 위해 명시 필드로 분리)
     */
    data class Partial(
        val name: String? = null,
        val price: Money? = null,
        val imageUrl: String? = null,
        val mall: Mall? = null,
        val parserUsed: ParserName,
        val fallbackUsed: Boolean = false,
    ) : ParseResult {
        /** 추출된 필드만 wire-friendly 키로 모은 맵 (presentation 직렬화 용). */
        fun toFieldMap(): Map<String, Any> = buildMap {
            name?.let { put("name", it) }
            price?.let { put("price", it) }
            imageUrl?.let { put("imageUrl", it) }
            mall?.let { put("mall", it) }
        }

        fun hasAnyField(): Boolean =
            name != null || price != null || imageUrl != null || mall != null
    }

    data class Failure(
        val reason: String,
        val parserUsed: ParserName,
    ) : ParseResult
}
