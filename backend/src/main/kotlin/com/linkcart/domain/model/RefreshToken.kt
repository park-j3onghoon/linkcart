package com.linkcart.domain.model

import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID? = null,
    val userId: Long,
    val tokenHash: String,
    val issuedAt: Instant? = null,
    val expiresAt: Instant,
    val revokedAt: Instant? = null,
    val replacedByTokenId: UUID? = null,
) {
    val isActive: Boolean get() = revokedAt == null
    fun isExpiredAt(now: Instant): Boolean = !now.isBefore(expiresAt)
}
