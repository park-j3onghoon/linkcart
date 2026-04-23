package com.linkcart.infrastructure.adapter.persistence.refreshtoken

import com.linkcart.domain.model.RefreshToken
import com.linkcart.domain.port.RefreshTokenRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class RefreshTokenRepositoryAdapter(
    private val jpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenRepository {

    override fun save(token: RefreshToken): RefreshToken =
        jpaRepository.saveAndFlush(token.toEntity()).toDomain()

    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        jpaRepository.findByTokenHash(tokenHash)?.toDomain()

    @Transactional
    override fun revokeAllActiveForUser(userId: Long, revokedAt: Instant): Int =
        jpaRepository.revokeAllActiveForUser(userId, revokedAt)

    @Transactional
    override fun markRevoked(id: UUID, revokedAt: Instant, replacedByTokenId: UUID?): Int =
        jpaRepository.markRevoked(id, revokedAt, replacedByTokenId)
}
