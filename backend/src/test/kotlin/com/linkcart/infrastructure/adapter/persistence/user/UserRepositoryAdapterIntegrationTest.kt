package com.linkcart.infrastructure.adapter.persistence.user

import com.linkcart.domain.entity.User
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.port.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@Disabled(
    "Testcontainers + Rancher Desktop docker socket 환경 설정 이슈로 인해 비활성화. " +
        "Postgres 연결 환경 해결 후 재활성화 예정 (후속 PR 또는 다음 세션).",
)
class UserRepositoryAdapterIntegrationTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @BeforeEach
    fun cleanup() {
        userJpaRepository.deleteAll()
    }

    @Test
    fun `save then findById returns same user`() {
        val saved = userRepository.save(newUser(providerUserId = "google-sub-123"))

        val found = userRepository.findById(saved.id!!)

        assertNotNull(found)
        assertEquals(AuthProvider.GOOGLE, found.provider)
        assertEquals("google-sub-123", found.providerUserId)
        assertEquals("user@example.com", found.email)
    }

    @Test
    fun `findById returns null when not found`() {
        assertNull(userRepository.findById(999_999L))
    }

    @Test
    fun `findByProviderAndProviderUserId returns null when not found`() {
        assertNull(userRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "missing"))
    }

    @Test
    fun `duplicate provider and providerUserId violates unique constraint`() {
        userRepository.save(newUser(providerUserId = "google-sub-456"))

        assertThrows<DataIntegrityViolationException> {
            userRepository.save(newUser(providerUserId = "google-sub-456", email = "other@example.com"))
        }
    }

    @Test
    fun `saved user has createdAt and updatedAt populated by DB`() {
        val saved = userRepository.save(newUser(providerUserId = "google-sub-789"))

        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    private fun newUser(providerUserId: String, email: String = "user@example.com"): User = User(
        provider = AuthProvider.GOOGLE,
        providerUserId = providerUserId,
        email = email,
        displayName = "테스트 사용자",
        avatarUrl = "https://example.com/avatar.jpg",
    )
}
