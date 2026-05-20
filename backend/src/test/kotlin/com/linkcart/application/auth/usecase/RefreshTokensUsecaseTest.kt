package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.RefreshTokenGenerator
import com.linkcart.domain.model.RefreshToken
import com.linkcart.domain.vo.AccessToken
import com.linkcart.domain.port.RefreshTokenRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshTokensUsecaseTest {

    @Test
    fun `happy path rotates refresh and revokes old`() {
        val now = Instant.parse("2026-04-17T00:00:00Z")
        val oldId = UUID.randomUUID()
        val repo = StubRefreshRepo(
            byHash = mapOf(
                "hash-raw-old" to RefreshToken(
                    id = oldId,
                    userId = 7L,
                    tokenHash = "hash-raw-old",
                    expiresAt = now.plusSeconds(60),
                ),
            ),
        )
        val sut = RefreshTokensUsecase(
            refreshTokenRepository = repo,
            refreshTokenGenerator = FixedHashGenerator(),
            issueTokensUsecase = StubIssueTokens(userId = 7L, access = "new-access"),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        val result = sut.execute("raw-old")

        assertEquals("new-access", result.accessToken.token)
        // 새 토큰 저장은 StubIssueTokens 내부 repo, revoke 만 outer repo 로 들어온다.
        assertEquals(1, repo.saved.size)
        val revokedSave = repo.saved.single()
        assertEquals(oldId, revokedSave.id)
        assertEquals(now, revokedSave.revokedAt)
    }

    @Test
    fun `expired refresh token throws`() {
        val now = Instant.parse("2026-04-17T00:00:00Z")
        val repo = StubRefreshRepo(
            byHash = mapOf(
                "hash-raw-old" to RefreshToken(
                    id = UUID.randomUUID(),
                    userId = 1L,
                    tokenHash = "hash-raw-old",
                    expiresAt = now.minusSeconds(1),
                ),
            ),
        )
        val sut = RefreshTokensUsecase(repo, FixedHashGenerator(), StubIssueTokens(1L, "x"), Clock.fixed(now, ZoneOffset.UTC))

        assertThrows<InvalidRefreshTokenException> { sut.execute("raw-old") }
    }

    @Test
    fun `reuse of revoked token triggers family revoke`() {
        val now = Instant.parse("2026-04-17T00:00:00Z")
        val repo = StubRefreshRepo(
            byHash = mapOf(
                "hash-raw-old" to RefreshToken(
                    id = UUID.randomUUID(),
                    userId = 9L,
                    tokenHash = "hash-raw-old",
                    expiresAt = now.plusSeconds(60),
                    revokedAt = now.minusSeconds(10),
                ),
            ),
        )
        val sut = RefreshTokensUsecase(repo, FixedHashGenerator(), StubIssueTokens(9L, "x"), Clock.fixed(now, ZoneOffset.UTC))

        assertThrows<InvalidRefreshTokenException> { sut.execute("raw-old") }
        assertTrue(repo.revokeAllForUserIds.contains(9L))
    }

    @Test
    fun `unknown refresh token throws`() {
        val now = Instant.parse("2026-04-17T00:00:00Z")
        val repo = StubRefreshRepo()
        val sut = RefreshTokensUsecase(repo, FixedHashGenerator(), StubIssueTokens(1L, "x"), Clock.fixed(now, ZoneOffset.UTC))

        assertThrows<InvalidRefreshTokenException> { sut.execute("raw-missing") }
    }

    private class FixedHashGenerator : RefreshTokenGenerator {
        override fun generate(): String = "generated-raw"
        override fun hash(rawToken: String): String = "hash-$rawToken"
    }

    private class StubRefreshRepo(
        private val byHash: Map<String, RefreshToken> = emptyMap(),
    ) : RefreshTokenRepository {
        val saved = mutableListOf<RefreshToken>()
        val revokeAllForUserIds = mutableListOf<Long>()

        override fun save(token: RefreshToken): RefreshToken {
            saved += token
            return token
        }
        override fun findByTokenHash(tokenHash: String): RefreshToken? = byHash[tokenHash]
        override fun revokeAllActiveForUser(userId: Long, revokedAt: Instant) {
            revokeAllForUserIds += userId
        }
    }

    private class StubIssueTokens(val userId: Long, val access: String) : IssueTokensUsecase(
        accessTokenIssuer = object : AccessTokenIssuer {
            override fun issue(userId: Long): AccessToken = AccessToken(access, Instant.EPOCH.plusSeconds(900))
            override fun verify(token: String): Long = error("unused")
        },
        refreshTokenGenerator = object : RefreshTokenGenerator {
            override fun generate(): String = "new-raw"
            override fun hash(rawToken: String): String = "new-hash"
        },
        refreshTokenRepository = object : RefreshTokenRepository {
            override fun save(token: RefreshToken): RefreshToken = token.copy(id = UUID.randomUUID())
            override fun findByTokenHash(tokenHash: String): RefreshToken? = null
            override fun revokeAllActiveForUser(userId: Long, revokedAt: Instant) {}
        },
    )
}
