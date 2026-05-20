package com.linkcart.domain.model

import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import com.linkcart.domain.vo.ParserName

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
        fun hasAnyField(): Boolean =
            name != null || price != null || imageUrl != null || mall != null
    }

    data class Failure(
        val reason: String,
        val parserUsed: ParserName,
    ) : ParseResult
}
