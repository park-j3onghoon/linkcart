package com.linkcart.infrastructure.adapter.persistence.sharelist

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.vo.Money

internal fun ShareListEntity.toDomain(): ShareList = ShareList(
    id = id,
    ownerId = ownerId,
    token = token,
    title = title,
    expiresAt = expiresAt,
    createdAt = createdAt,
    items = items.map { it.toDomain() },
)

internal fun ShareList.toEntity(): ShareListEntity = ShareListEntity(
    id = id,
    ownerId = ownerId,
    token = token,
    title = title,
    expiresAt = expiresAt,
    createdAt = createdAt,
    items = items.map { it.toEntity() }.toMutableList(),
)

internal fun ShareListItemEntity.toDomain(): ShareListItem = ShareListItem(
    id = id,
    name = name,
    price = Money(amount = priceAmount, currency = priceCurrency),
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    mall = mall,
)

internal fun ShareListItem.toEntity(): ShareListItemEntity = ShareListItemEntity(
    id = id,
    name = name,
    priceAmount = price.amount,
    priceCurrency = price.currency,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    mall = mall,
)
