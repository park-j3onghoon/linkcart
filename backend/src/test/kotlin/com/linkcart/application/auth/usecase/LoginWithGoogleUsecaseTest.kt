package com.linkcart.application.auth.usecase

import com.linkcart.application.auth.port.AccessTokenIssuer
import com.linkcart.application.auth.port.GoogleOAuthClient
import com.linkcart.application.auth.port.GoogleOAuthException
import com.linkcart.domain.entity.User
import com.linkcart.domain.model.AccessToken
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.model.GoogleIdentity
import com.linkcart.domain.port.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals

class LoginWithGoogleUsecaseTest {

    @Test
    fun `returns token and user when google identity maps to existing user`() {
        val identity = GoogleIdentity("sub-1", "a@example.com", "A", null)
        val existing = User(id = 10L, provider = AuthProvider.GOOGLE, providerUserId = "sub-1", email = "a@example.com")

        val useCase = LoginWithGoogleUsecase(
            googleOAuthClient = StubGoogleClient(identity),
            userRepository = StubUserRepository(byProviderSub = mapOf("sub-1" to existing)),
            accessTokenIssuer = StubIssuer(issued = AccessToken("token-10", Instant.EPOCH.plusSeconds(900))),
        )

        val result = useCase.execute("code", "https://app/callback")

        assertEquals(10L, result.user.id)
        assertEquals("token-10", result.accessToken.token)
    }

    @Test
    fun `creates new user when google subject is unknown`() {
        val identity = GoogleIdentity("sub-new", "new@example.com", "New", "https://a/p")
        val savedUser = User(id = 99L, provider = AuthProvider.GOOGLE, providerUserId = "sub-new", email = "new@example.com")

        val repository = StubUserRepository(savedId = savedUser)
        val useCase = LoginWithGoogleUsecase(
            googleOAuthClient = StubGoogleClient(identity),
            userRepository = repository,
            accessTokenIssuer = StubIssuer(issued = AccessToken("token-99", Instant.EPOCH.plusSeconds(900))),
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
            accessTokenIssuer = StubIssuer(AccessToken("unused", Instant.EPOCH)),
        )

        assertThrows<GoogleOAuthException> { useCase.execute("bad", "https://app/callback") }
    }

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

    private class StubIssuer(private val issued: AccessToken) : AccessTokenIssuer {
        override fun issue(userId: Long): AccessToken = issued
        override fun verify(token: String): Long = error("not used")
    }
}
