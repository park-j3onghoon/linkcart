package com.linkcart.presentation.dto

import com.linkcart.domain.model.User
import com.linkcart.domain.model.AuthProvider

data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
    val provider: AuthProvider,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = requireNotNull(user.id) { "UserResponse requires saved user" },
            email = user.email,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            provider = user.provider,
        )
    }
}
