package com.linkcart.infrastructure.adapter.persistence.user

import com.linkcart.domain.entity.User

internal fun UserEntity.toDomain(): User = User(
    id = id,
    provider = provider,
    providerUserId = providerUserId,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    provider = provider,
    providerUserId = providerUserId,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
