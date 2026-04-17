package com.linkcart.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ParseRequest(
    @field:NotBlank(message = "URL은 비어 있을 수 없습니다")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "http:// 또는 https:// URL이어야 합니다",
    )
    val url: String,
)
