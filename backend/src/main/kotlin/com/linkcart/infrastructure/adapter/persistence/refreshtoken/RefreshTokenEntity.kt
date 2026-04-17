package com.linkcart.infrastructure.adapter.persistence.refreshtoken

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    val tokenHash: String,

    @Column(name = "issued_at", nullable = false, insertable = false, updatable = false)
    val issuedAt: Instant? = null,

    @Column(name = "expires_at", nullable = false, updatable = false)
    val expiresAt: Instant,

    @Column(name = "revoked_at")
    val revokedAt: Instant? = null,

    @Column(name = "replaced_by_token_id")
    val replacedByTokenId: UUID? = null,
)
