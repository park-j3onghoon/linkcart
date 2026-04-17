package com.linkcart.presentation.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.linkcart.domain.entity.UserProduct
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SaveUserProductRequest(
    @field:NotBlank
    val name: String,
    @field:NotNull @field:Valid
    val price: PriceDto,
    val imageUrl: String?,
    @field:NotBlank
    @field:Pattern(regexp = "^https?://.+", message = "source_url은 http(s)로 시작해야 합니다")
    val sourceUrl: String,
    @field:NotNull
    val mall: Mall,
    @field:NotNull
    val parserUsed: ParserName,
) {
    fun toDomain(userId: Long): UserProduct = UserProduct(
        userId = userId,
        name = name,
        price = Money(amount = price.amount, currency = price.currency),
        imageUrl = imageUrl,
        sourceUrl = sourceUrl,
        mall = mall,
        parserUsed = parserUsed,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class PriceDto(
        @field:PositiveOrZero
        val amount: Long,
        @field:NotBlank
        val currency: String = "KRW",
    )
}
