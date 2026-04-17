package com.linkcart.infrastructure.adapter.persistence.refreshtoken

import com.linkcart.domain.entity.RefreshToken
import java.util.UUID

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
    id = id ?: UUID.randomUUID(),
    userId = userId,
    tokenHash = tokenHash,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
    replacedByTokenId = replacedByTokenId,
)
