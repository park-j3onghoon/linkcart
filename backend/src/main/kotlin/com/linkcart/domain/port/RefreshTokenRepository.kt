package com.linkcart.domain.port

import com.linkcart.domain.model.RefreshToken
import java.time.Instant

interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun revokeAllActiveForUser(userId: Long, revokedAt: Instant)
}
