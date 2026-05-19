package com.linkcart.presentation.dto

import jakarta.validation.constraints.NotBlank

data class LogoutRequest(
    @field:NotBlank(message = "refreshToken은 비어 있을 수 없습니다")
    val refreshToken: String,
)
