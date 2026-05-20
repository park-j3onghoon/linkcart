package com.linkcart.presentation.dto

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money

/**
 * `:parse`는 method 응답이라 AIP-148(name=리소스 path) 적용 안 함.
 * 여기의 `name`은 상품명(display name)이며 리소스 응답의 `name`(리소스 path)과 의미가 다르다.
 */
data class ParseResponse(
    val name: String?,
    val price: Money?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val mall: Mall?,
    val partial: Map<String, Any>?,
    val parserUsed: ParserName,
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
                    name = result.name,
                    price = result.price,
                    imageUrl = result.imageUrl,
                    sourceUrl = requestedUrl,
                    mall = result.mall,
                    partial = result.toFieldMap(),
                    parserUsed = result.parserUsed,
                    fallbackUsed = result.fallbackUsed,
                )

                is ParseResult.Failure -> throw IllegalArgumentException("Failure result cannot be converted to ParseResponse")
            }
    }
}
