package com.linkcart.domain.port

import com.linkcart.domain.entity.UserProduct

interface UserProductRepository {
    fun save(product: UserProduct): UserProduct
    fun findById(id: Long): UserProduct?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<UserProduct>
    fun deleteById(id: Long)
    fun existsByUserIdAndSourceUrl(userId: Long, sourceUrl: String): Boolean
}
