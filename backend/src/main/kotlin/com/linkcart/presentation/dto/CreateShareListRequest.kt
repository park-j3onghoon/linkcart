package com.linkcart.presentation.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateShareListRequest(
    @field:NotEmpty(message = "product_ids는 1개 이상이어야 합니다")
    val productIds: List<Long>,
    @field:Size(max = 200, message = "title은 200자 이하여야 합니다")
    val title: String? = null,
    val expiresAt: Instant? = null,
)
