package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.RefreshTokenGenerator
import com.linkcart.domain.model.RefreshToken
import com.linkcart.domain.vo.AccessToken
import com.linkcart.domain.port.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Service
class IssueTokensUsecase(
    private val accessTokenIssuer: AccessTokenIssuer,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val clock: Clock = Clock.systemUTC(),
    @Value("\${linkcart.jwt.refresh-token-ttl-days}")
    private val refreshTokenTtlDays: Long = 14,
) {

    fun execute(userId: Long, replacedByTokenId: UUID? = null): IssueResult {
        val access = accessTokenIssuer.issue(userId)
        val rawRefresh = refreshTokenGenerator.generate()
        val refreshHash = refreshTokenGenerator.hash(rawRefresh)

        // ID 부여는 도메인/usecase 책임. 매퍼가 비결정적 난수를 만들지 않도록.
        val tokenId = UUID.randomUUID()
        refreshTokenRepository.save(
            RefreshToken(
                id = tokenId,
                userId = userId,
                tokenHash = refreshHash,
                expiresAt = clock.instant().plus(Duration.ofDays(refreshTokenTtlDays)),
            ),
        )

        return IssueResult(accessToken = access, rawRefreshToken = rawRefresh, refreshTokenId = tokenId)
    }

    data class IssueResult(
        val accessToken: AccessToken,
        val rawRefreshToken: String,
        val refreshTokenId: UUID,
    )
}
