package com.linkcart.infrastructure.adapter.persistence.user

import com.linkcart.domain.model.User
import com.linkcart.domain.model.AuthProvider
import com.linkcart.domain.port.UserRepository
import org.springframework.stereotype.Component

@Component
class UserRepositoryAdapter(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): User? =
        userJpaRepository.findByProviderAndProviderUserId(provider, providerUserId)?.toDomain()

    override fun findById(id: Long): User? =
        userJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun save(user: User): User =
        userJpaRepository.saveAndFlush(user.toEntity()).toDomain()
}
