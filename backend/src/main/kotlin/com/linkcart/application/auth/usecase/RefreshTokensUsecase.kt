package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.RefreshTokenGenerator
import com.linkcart.domain.port.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

class InvalidRefreshTokenException(message: String) : RuntimeException(message)

@Service
class RefreshTokensUsecase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    private val issueTokensUsecase: IssueTokensUsecase,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional
    fun execute(rawRefreshToken: String): IssueTokensUsecase.IssueResult {
        val now = clock.instant()
        val tokenHash = refreshTokenGenerator.hash(rawRefreshToken)
        val existing = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw InvalidRefreshTokenException("Refresh token을 찾을 수 없습니다")

        if (existing.isExpiredAt(now)) {
            throw InvalidRefreshTokenException("Refresh token이 만료되었습니다")
        }

        if (!existing.isActive) {
            // Reuse 감지: 동일 user의 모든 active refresh token 취소.
            refreshTokenRepository.revokeAllActiveForUser(existing.userId, now)
            throw InvalidRefreshTokenException("Refresh token 재사용이 감지되었습니다")
        }

        val newIssue = issueTokensUsecase.execute(existing.userId)
        refreshTokenRepository.save(existing.revoked(at = now, replacedBy = newIssue.refreshTokenId))
        return newIssue
    }
}
