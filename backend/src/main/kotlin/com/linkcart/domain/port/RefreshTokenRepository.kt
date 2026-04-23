package com.linkcart.domain.port

import com.linkcart.domain.model.RefreshToken
import java.time.Instant

interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun revokeAllActiveForUser(userId: Long, revokedAt: Instant): Int
    fun markRevoked(id: java.util.UUID, revokedAt: Instant, replacedByTokenId: java.util.UUID?): Int
}
