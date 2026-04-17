package com.linkcart.presentation.dto

import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:NotBlank(message = "code는 비어 있을 수 없습니다")
    val code: String,
    @field:NotBlank(message = "redirectUri는 비어 있을 수 없습니다")
    val redirectUri: String,
)
