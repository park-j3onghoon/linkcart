package com.linkcart.infrastructure.adapter.persistence.user

import com.linkcart.domain.model.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): UserEntity?
}
