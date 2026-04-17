package com.linkcart.domain.model

data class GoogleIdentity(
    val subject: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
)
