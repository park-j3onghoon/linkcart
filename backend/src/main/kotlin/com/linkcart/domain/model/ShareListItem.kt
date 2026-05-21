package com.linkcart.domain.model

import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money

data class ShareListItem(
    val id: Long? = null,
    val name: String,
    val price: Money,
    val imageUrl: String?,
    val sourceUrl: String,
    val mall: Mall,
)
