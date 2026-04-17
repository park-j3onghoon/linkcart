package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.GoogleOAuthClient
import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.application.auth.port.RefreshTokenGenerator
import com.linkcart.domain.entity.RefreshToken
import com.linkcart.domain.entity.User
import com.linkcart.domain.model.AccessToken
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.model.GoogleIdentity
import com.linkcart.domain.port.RefreshTokenRepository
import com.linkcart.domain.port.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class LoginWithGoogleUsecaseTest {

    @Test
    fun `returns tokens and user when google identity maps to existing user`() {
        val identity = GoogleIdentity("sub-1", "a@example.com", "A", null)
        val existing = User(id = 10L, provider = AuthProvider.GOOGLE, providerUserId = "sub-1", email = "a@example.com")

        val useCase = LoginWithGoogleUsecase(
            googleOAuthClient = StubGoogleClient(identity),
            userRepository = StubUserRepository(byProviderSub = mapOf("sub-1" to existing)),
            issueTokensUsecase = stubIssueTokens(),
        )

        val result = useCase.execute("code", "https://app/callback")

        assertEquals(10L, result.user.id)
        assertEquals("issued-access", result.tokens.accessToken.token)
        assertEquals("issued-refresh", result.tokens.rawRefreshToken)
    }

    @Test
    fun `creates new user when google subject is unknown`() {
        val identity = GoogleIdentity("sub-new", "new@example.com", "New", null)
        val savedUser = User(id = 99L, provider = AuthProvider.GOOGLE, providerUserId = "sub-new", email = "new@example.com")
        val repository = StubUserRepository(savedId = savedUser)

        val useCase = LoginWithGoogleUsecase(
            googleOAuthClient = StubGoogleClient(identity),
            userRepository = repository,
            issueTokensUsecase = stubIssueTokens(),
        )

        val result = useCase.execute("code", "https://app/callback")

        assertEquals(99L, result.user.id)
        assertEquals(1, repository.savedCount)
    }

    @Test
    fun `propagates GoogleOAuthException when code exchange fails`() {
        val useCase = LoginWithGoogleUsecase(
            googleOAuthClient = ThrowingGoogleClient(GoogleOAuthException("boom")),
            userRepository = StubUserRepository(),
            issueTokensUsecase = stubIssueTokens(),
        )

        assertThrows<GoogleOAuthException> { useCase.execute("bad", "https://app/callback") }
    }

    private fun stubIssueTokens(): IssueTokensUsecase = IssueTokensUsecase(
        accessTokenIssuer = object : AccessTokenIssuer {
            override fun issue(userId: Long): AccessToken = AccessToken("issued-access", Instant.EPOCH.plusSeconds(900))
            override fun verify(token: String): Long = error("not used")
        },
        refreshTokenGenerator = object : RefreshTokenGenerator {
            override fun generate(): String = "issued-refresh"
            override fun hash(rawToken: String): String = "hash"
        },
        refreshTokenRepository = object : RefreshTokenRepository {
            override fun save(token: RefreshToken): RefreshToken = token.copy(id = UUID.randomUUID())
            override fun findByTokenHash(tokenHash: String): RefreshToken? = null
            override fun revokeAllActiveForUser(userId: Long, revokedAt: Instant): Int = 0
            override fun markRevoked(id: UUID, revokedAt: Instant, replacedByTokenId: UUID?): Int = 0
        },
    )

    private class StubGoogleClient(private val identity: GoogleIdentity) : GoogleOAuthClient {
        override fun exchangeCodeForIdentity(code: String, redirectUri: String): GoogleIdentity = identity
    }

    private class ThrowingGoogleClient(private val ex: RuntimeException) : GoogleOAuthClient {
        override fun exchangeCodeForIdentity(code: String, redirectUri: String): GoogleIdentity = throw ex
    }

    private class StubUserRepository(
        private val byProviderSub: Map<String, User> = emptyMap(),
        private val savedId: User? = null,
    ) : UserRepository {
        var savedCount = 0
            private set

        override fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): User? =
            byProviderSub[providerUserId]

        override fun findById(id: Long): User? = null

        override fun save(user: User): User {
            savedCount += 1
            return savedId ?: user.copy(id = 1L)
        }
    }
}
