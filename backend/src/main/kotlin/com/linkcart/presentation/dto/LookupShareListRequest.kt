package com.linkcart.presentation.dto

import jakarta.validation.constraints.NotBlank

data class LookupShareListRequest(
    @field:NotBlank
    val token: String,
)
