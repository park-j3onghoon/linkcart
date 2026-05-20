package com.linkcart.presentation.dto

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.vo.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero

data class SaveUserProductRequest(
    @field:NotBlank
    val displayName: String,
    @field:NotNull @field:Valid
    val price: PriceDto,
    val imageUrl: String?,
    @field:NotBlank
    @field:Pattern(regexp = ValidationPatterns.HTTP_URL, message = "sourceUrl은 http(s)로 시작해야 합니다")
    val sourceUrl: String,
    @field:NotNull
    val mall: Mall,
    @field:NotNull
    val parserUsed: ParserName,
) {
    fun toDomain(userId: Long): UserProduct = UserProduct(
        userId = userId,
        name = displayName,
        price = Money(amount = price.amount, currency = price.currency),
        imageUrl = imageUrl,
        sourceUrl = sourceUrl,
        mall = mall,
        parserUsed = parserUsed,
    )

    data class PriceDto(
        @field:PositiveOrZero
        val amount: Long,
        @field:NotBlank
        val currency: String = Money.DEFAULT_CURRENCY,
    )
}
