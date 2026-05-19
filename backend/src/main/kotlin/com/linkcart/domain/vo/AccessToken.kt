package com.linkcart.domain.vo

import java.time.Instant

data class AccessToken(
    val token: String,
    val expiresAt: Instant,
)
