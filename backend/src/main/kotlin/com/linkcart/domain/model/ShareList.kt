package com.linkcart.domain.model

import java.time.Instant

data class ShareList(
    val id: Long? = null,
    val ownerId: Long,
    val token: String,
    val title: String? = null,
    val expiresAt: Instant? = null,
    val createdAt: Instant? = null,
    val items: List<ShareListItem> = emptyList(),
) {
    fun isExpired(at: Instant): Boolean {
        val expiry = expiresAt ?: return false
        return !at.isBefore(expiry)
    }
}
