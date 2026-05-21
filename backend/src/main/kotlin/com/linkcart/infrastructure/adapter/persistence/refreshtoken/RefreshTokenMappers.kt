package com.linkcart.infrastructure.adapter.persistence.refreshtoken

import com.linkcart.domain.model.RefreshToken

internal fun RefreshTokenEntity.toDomain(): RefreshToken = RefreshToken(
    id = id,
    userId = userId,
    tokenHash = tokenHash,
    issuedAt = issuedAt,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    replacedByTokenId = replacedByTokenId,
)

internal fun RefreshToken.toEntity(): RefreshTokenEntity = RefreshTokenEntity(
    id = requireNotNull(id) { "RefreshToken.id must be set before persistence (assign at construction)" },
    userId = userId,
    tokenHash = tokenHash,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    replacedByTokenId = replacedByTokenId,
)
