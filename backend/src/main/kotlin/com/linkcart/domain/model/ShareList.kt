package com.linkcart.domain.model

import java.time.Instant

class EmptyShareListException(message: String) : RuntimeException(message)

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

    fun isOwnedBy(ownerId: Long): Boolean = this.ownerId == ownerId

    fun hasId(id: Long): Boolean = this.id == id

    companion object {
        /**
         * 신규 ShareList 생성 invariant 강제 — items 가 비어 있으면 거부.
         * (영속화된 ShareList 의 items 가 일시적으로 비어 있는 경우는 허용되어야 하므로
         * 기본 생성자가 아닌 factory 에서만 검증한다.)
         */
        fun create(
            ownerId: Long,
            token: String,
            items: List<ShareListItem>,
            title: String? = null,
            expiresAt: Instant? = null,
        ): ShareList {
            if (items.isEmpty()) {
                throw EmptyShareListException("공유할 상품을 1개 이상 선택해주세요")
            }
            return ShareList(
                ownerId = ownerId,
                token = token,
                title = title,
                expiresAt = expiresAt,
                items = items,
            )
        }
    }
}
