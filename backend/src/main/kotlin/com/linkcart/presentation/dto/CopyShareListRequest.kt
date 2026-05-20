package com.linkcart.presentation.dto

import jakarta.validation.constraints.NotBlank

/** token-as-capability: path의 {id}와 token이 가리키는 ShareList가 일치해야 통과 (AIP-131). */
data class CopyShareListRequest(
    @field:NotBlank
    val token: String,
)
