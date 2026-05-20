package com.linkcart.infrastructure.adapter.persistence.refreshtoken

import com.linkcart.domain.model.RefreshToken
import com.linkcart.domain.port.RefreshTokenRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class RefreshTokenRepositoryAdapter(
    private val jpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenRepository {

    override fun save(token: RefreshToken): RefreshToken =
        jpaRepository.saveAndFlush(token.toEntity()).toDomain()

    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        jpaRepository.findByTokenHash(tokenHash)?.toDomain()

    @Transactional
    override fun revokeAllActiveForUser(userId: Long, revokedAt: Instant) {
        jpaRepository.revokeAllActiveForUser(userId, revokedAt)
    }
}
