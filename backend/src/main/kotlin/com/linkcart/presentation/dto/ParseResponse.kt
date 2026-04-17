package com.linkcart.presentation.dto

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money

data class ParseResponse(
    val name: String?,
    val price: Money?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val mall: Mall?,
    val partial: Map<String, Any>?,
    val parserUsed: String,
    val fallbackUsed: Boolean,
) {
    companion object {
        fun from(result: ParseResult, requestedUrl: String): ParseResponse =
            when (result) {
                is ParseResult.Success -> ParseResponse(
                    name = result.product.name,
                    price = result.product.price,
                    imageUrl = result.product.imageUrl,
                    sourceUrl = result.product.sourceUrl,
                    mall = result.product.mall,
                    partial = null,
                    parserUsed = result.parserUsed,
                    fallbackUsed = result.fallbackUsed,
                )

                is ParseResult.Partial -> ParseResponse(
                    name = result.fields["name"] as? String,
                    price = result.fields["price"] as? Money,
                    imageUrl = result.fields["imageUrl"] as? String,
                    sourceUrl = requestedUrl,
                    mall = result.fields["mall"] as? Mall,
                    partial = result.fields.mapKeys { (key, _) -> toResponseFieldName(key) },
                    parserUsed = result.parserUsed,
                    fallbackUsed = result.fallbackUsed,
                )

                is ParseResult.Failure -> throw IllegalArgumentException("Failure result cannot be converted to ParseResponse")
            }

        private fun toResponseFieldName(key: String): String =
            when (key) {
                "imageUrl" -> "image_url"
                "sourceUrl" -> "source_url"
                "parserUsed" -> "parser_used"
                "fallbackUsed" -> "fallback_used"
                else -> key
            }
    }
}
