package com.linkcart.domain.model

import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money

sealed interface ParseResult {
    data class Success(
        val product: Product,
        val parserUsed: ParserName,
        val fallbackUsed: Boolean = false,
    ) : ParseResult

    data class Partial(
        val name: String? = null,
        val price: Money? = null,
        val imageUrl: String? = null,
        val mall: Mall? = null,
        val parserUsed: ParserName,
        val fallbackUsed: Boolean = false,
    ) : ParseResult {
        /** presentation 직렬화용. 추출된 필드만 골라낸다. */
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
