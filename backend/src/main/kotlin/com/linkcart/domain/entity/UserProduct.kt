package com.linkcart.domain.entity

import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import java.time.Instant

data class UserProduct(
    val id: Long? = null,
    val userId: Long,
    val name: String,
    val price: Money,
    val imageUrl: String?,
    val sourceUrl: String,
    val mall: Mall,
    val parserUsed: ParserName,
    val createdAt: Instant? = null,
)
