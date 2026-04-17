package com.linkcart.domain.entity

import com.linkcart.domain.model.AuthProvider
import java.time.Instant

data class User(
    val id: Long? = null,
    val provider: AuthProvider,
    val providerUserId: String,
    val email: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
