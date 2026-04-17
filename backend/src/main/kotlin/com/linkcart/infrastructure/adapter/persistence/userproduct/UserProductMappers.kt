package com.linkcart.infrastructure.adapter.persistence.userproduct

import com.linkcart.domain.entity.UserProduct
import com.linkcart.domain.vo.Money

internal fun UserProductEntity.toDomain(): UserProduct = UserProduct(
    id = id,
    userId = userId,
    name = name,
    price = Money(amount = priceAmount, currency = priceCurrency),
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    mall = mall,
    parserUsed = parserUsed,
    createdAt = createdAt,
)

internal fun UserProduct.toEntity(): UserProductEntity = UserProductEntity(
    id = id,
    userId = userId,
    name = name,
    priceAmount = price.amount,
    priceCurrency = price.currency,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    mall = mall,
    parserUsed = parserUsed,
    createdAt = createdAt,
)
