package com.linkcart.presentation.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import java.time.Instant

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UserProductResponse(
    val id: Long,
    val name: String,
    val price: Money,
    val imageUrl: String?,
    val sourceUrl: String,
    val mall: Mall,
    val parserUsed: ParserName,
    val createdAt: Instant?,
) {
    companion object {
        fun from(product: UserProduct): UserProductResponse = UserProductResponse(
            id = requireNotNull(product.id),
            name = product.name,
            price = product.price,
            imageUrl = product.imageUrl,
            sourceUrl = product.sourceUrl,
            mall = product.mall,
            parserUsed = product.parserUsed,
            createdAt = product.createdAt,
        )
    }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UserProductsResponse(
    val products: List<UserProductResponse>,
)
