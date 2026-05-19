package com.linkcart.domain.vo

data class GoogleIdentity(
    val subject: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
)
