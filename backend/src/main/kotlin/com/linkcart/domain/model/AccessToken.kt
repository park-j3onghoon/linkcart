package com.linkcart.domain.model

import java.time.Instant

data class AccessToken(
    val token: String,
    val expiresAt: Instant,
)
