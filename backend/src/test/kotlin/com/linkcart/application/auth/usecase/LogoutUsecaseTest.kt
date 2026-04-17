package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.RefreshTokenGenerator
import com.linkcart.domain.entity.RefreshToken
import com.linkcart.domain.port.RefreshTokenRepository
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class LogoutUsecaseTest {

    @Test
    fun `revokes active refresh token`() {
        val now = Instant.parse("2026-04-17T00:00:00Z")
        val tokenId = UUID.randomUUID()
        val repo = StubRepo(
            byHash = mapOf(
                "hash-raw" to RefreshToken(
                    id = tokenId, userId = 1L, tokenHash = "hash-raw",
                    expiresAt = now.plusSeconds(60),
                ),
            ),
        )
        val sut = LogoutUsecase(repo, FixedHashGenerator(), Clock.fixed(now, ZoneOffset.UTC))

        sut.execute("raw")

        assertEquals(1, repo.markRevokedCalls)
    }

    @Test
    fun `no-op when token does not exist`() {
        val sut = LogoutUsecase(StubRepo(), FixedHashGenerator(), Clock.systemUTC())
        sut.execute("missing")  // does not throw
    }

    @Test
    fun `no-op when token already revoked`() {
        val now = Instant.parse("2026-04-17T00:00:00Z")
        val repo = StubRepo(
            byHash = mapOf(
                "hash-raw" to RefreshToken(
                    id = UUID.randomUUID(), userId = 1L, tokenHash = "hash-raw",
                    expiresAt = now.plusSeconds(60), revokedAt = now.minusSeconds(1),
                ),
            ),
        )
        val sut = LogoutUsecase(repo, FixedHashGenerator(), Clock.fixed(now, ZoneOffset.UTC))

        sut.execute("raw")

        assertEquals(0, repo.markRevokedCalls)
    }

    private class FixedHashGenerator : RefreshTokenGenerator {
        override fun generate(): String = "x"
        override fun hash(rawToken: String): String = "hash-$rawToken"
    }

    private class StubRepo(
        private val byHash: Map<String, RefreshToken> = emptyMap(),
    ) : RefreshTokenRepository {
        var markRevokedCalls = 0
            private set

        override fun save(token: RefreshToken): RefreshToken = token
        override fun findByTokenHash(tokenHash: String): RefreshToken? = byHash[tokenHash]
        override fun revokeAllActiveForUser(userId: Long, revokedAt: Instant): Int = 0
        override fun markRevoked(id: UUID, revokedAt: Instant, replacedByTokenId: UUID?): Int {
            markRevokedCalls += 1
            return 1
        }
    }
}
