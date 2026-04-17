package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.RefreshTokenGenerator
import com.linkcart.domain.port.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class LogoutUsecase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional
    fun execute(rawRefreshToken: String) {
        val tokenHash = refreshTokenGenerator.hash(rawRefreshToken)
        val existing = refreshTokenRepository.findByTokenHash(tokenHash) ?: return
        if (!existing.isActive) return
        refreshTokenRepository.markRevoked(
            id = requireNotNull(existing.id),
            revokedAt = clock.instant(),
            replacedByTokenId = null,
        )
    }
}
