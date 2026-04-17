package com.linkcart.presentation.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OAuthLoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse,
)
