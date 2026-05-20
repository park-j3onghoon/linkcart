package com.linkcart.domain.port

import com.linkcart.domain.model.UserProduct
import java.time.Instant

interface UserProductRepository {
    fun save(product: UserProduct): UserProduct
    fun findById(id: Long): UserProduct?
    fun findPageByUserId(
        userId: Long,
        cursorCreatedAt: Instant?,
        cursorId: Long?,
        limit: Int,
    ): List<UserProduct>
    fun deleteById(id: Long)
    fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean
}
