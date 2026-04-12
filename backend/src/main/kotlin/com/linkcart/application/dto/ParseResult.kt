package com.linkcart.application.dto

import com.linkcart.domain.entity.Product

sealed class ParseResult {
    data class Success(
        val product: Product,
        val parserUsed: String,
        val fallbackUsed: Boolean = false,
    ) : ParseResult()

    data class Partial(
        val fields: Map<String, Any>,
        val parserUsed: String,
        val fallbackUsed: Boolean = false,
    ) : ParseResult()

    data class Failure(
        val reason: String,
        val parserUsed: String,
    ) : ParseResult()
}
