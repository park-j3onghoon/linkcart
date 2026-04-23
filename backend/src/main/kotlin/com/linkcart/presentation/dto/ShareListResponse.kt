package com.linkcart.presentation.dto

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import java.time.Instant

data class ShareListResponse(
    val id: Long,
    val token: String,
    val title: String?,
    val expiresAt: Instant?,
    val createdAt: Instant?,
    val items: List<ShareListItemResponse>,
) {
    companion object {
        fun from(shareList: ShareList): ShareListResponse = ShareListResponse(
            id = requireNotNull(shareList.id),
            token = shareList.token,
            title = shareList.title,
            expiresAt = shareList.expiresAt,
            createdAt = shareList.createdAt,
            items = shareList.items.map(ShareListItemResponse::from),
        )
    }
}

data class ShareListItemResponse(
    val id: Long,
    val name: String,
    val price: Money,
    val imageUrl: String?,
    val sourceUrl: String,
    val mall: Mall,
) {
    companion object {
        fun from(item: ShareListItem): ShareListItemResponse = ShareListItemResponse(
            id = requireNotNull(item.id),
            name = item.name,
            price = item.price,
            imageUrl = item.imageUrl,
            sourceUrl = item.sourceUrl,
            mall = item.mall,
        )
    }
}
