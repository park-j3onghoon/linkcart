package com.linkcart.presentation.dto

import com.linkcart.domain.model.User
import com.linkcart.domain.model.AuthProvider

/**
 * AIP-122 / AIP-148: 리소스는 path 형식의 `name`으로 식별한다.
 */
data class UserResponse(
    val name: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
    val provider: AuthProvider,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            name = "users/${requireNotNull(user.id) { "UserResponse requires saved user" }}",
            email = user.email,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            provider = user.provider,
        )
    }
}
