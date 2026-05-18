package com.linkcart.presentation.dto

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import java.time.Instant

/**
 * AIP-122 / AIP-148: name = "shareLists/{id}", time 필드는 RFC3339(create/expire).
 * 아이템은 부모-자식 path "shareLists/{parent}/items/{id}"로 식별한다.
 */
data class ShareListResponse(
    val name: String,
    val token: String,
    val title: String?,
    val expireTime: Instant?,
    val createTime: Instant?,
    val items: List<ShareListItemResponse>,
) {
    companion object {
        fun from(shareList: ShareList): ShareListResponse {
            val parentName = "shareLists/${requireNotNull(shareList.id)}"
            return ShareListResponse(
                name = parentName,
                token = shareList.token,
                title = shareList.title,
                expireTime = shareList.expiresAt,
                createTime = shareList.createdAt,
                items = shareList.items.map { ShareListItemResponse.from(it, parentName) },
            )
        }
    }
}

data class ShareListItemResponse(
    val name: String,
    val displayName: String,
    val price: Money,
    val imageUrl: String?,
    val sourceUrl: String,
    val mall: Mall,
) {
    companion object {
        fun from(item: ShareListItem, parentName: String): ShareListItemResponse = ShareListItemResponse(
            name = "$parentName/items/${requireNotNull(item.id)}",
            displayName = item.name,
            price = item.price,
            imageUrl = item.imageUrl,
            sourceUrl = item.sourceUrl,
            mall = item.mall,
        )
    }
}
