package com.linkcart.presentation.dto

data class CopyShareListResponse(
    val copiedCount: Int,
    val skippedCount: Int,
    val products: List<UserProductResponse>,
)
