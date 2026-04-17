package com.linkcart.domain.port

import com.linkcart.domain.entity.User
import com.linkcart.domain.model.AuthProvider

interface UserRepository {
    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): User?
    fun findById(id: Long): User?
    fun save(user: User): User
}
