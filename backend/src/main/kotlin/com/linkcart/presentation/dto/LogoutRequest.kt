package com.linkcart.presentation.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.validation.constraints.NotBlank

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LogoutRequest(
    @field:NotBlank(message = "refresh_token은 비어 있을 수 없습니다")
    val refreshToken: String,
)
