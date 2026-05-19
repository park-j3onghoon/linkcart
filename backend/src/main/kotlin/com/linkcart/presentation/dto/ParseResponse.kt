package com.linkcart.presentation.dto

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money

/**
 * `:parse`의 method 응답. 리소스 조회 응답이 아니므로 AIP-148(name=리소스 path) 규칙에 따르지 않는다.
 * 여기의 `name`은 파싱된 상품명(display name)이며, UserProduct/ShareList 등의 리소스 응답에서 쓰는 `name`(리소스 path)과 의미가 다르다.
 * 파싱 결과를 그대로 저장할 수 있도록 Product 필드를 top-level로 펼친 flat 구조를 유지한다.
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
