package com.linkcart.presentation.dto

import jakarta.validation.constraints.NotBlank

/**
 * AIP-131 / token-as-capability: 복제도 secret token 보유자만 가능하다.
 * path의 {id}와 body의 token이 가리키는 ShareList가 일치해야 통과.
 */
data class CopyShareListRequest(
    @field:NotBlank
    val token: String,
)
