package com.linkcart.presentation.dto

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money

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
        /**
         * 도메인 ParseResult를 wire 응답으로 변환.
         * 프론트 ParseResponse가 Product 필드를 top-level로 펼친 flat 구조라 nested "product"를 쓰지 않는다.
         */
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
